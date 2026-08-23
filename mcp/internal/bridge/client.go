package bridge

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"
)

const maxBody = 2 << 20
const bridgeURL = "http://127.0.0.1:8765"

type Client struct {
	url  string
	http *http.Client
}

type Error struct {
	StatusCode int
	Message    string
}

func (e *Error) Error() string {
	if e.StatusCode == 0 {
		return "Minecraft bridge unavailable: " + e.Message
	}
	return fmt.Sprintf("Minecraft bridge returned HTTP %d: %s", e.StatusCode, e.Message)
}

func New() *Client { return newAt(bridgeURL) }

func newAt(url string) *Client {
	return &Client{url: url, http: &http.Client{Timeout: 15 * time.Second}}
}

func (c *Client) Call(ctx context.Context, method string, args interface{}, out interface{}) error {
	body, err := c.CallRaw(ctx, method, args)
	if err != nil {
		return err
	}
	return json.Unmarshal(body, out)
}

// CallRaw returns the bridge's JSON response without imposing a Go response
// model. Most MCP tools forward this payload verbatim; callers that compute a
// derived result can decode it with Call instead.
func (c *Client) CallRaw(ctx context.Context, method string, args interface{}) ([]byte, error) {
	if len(method) > 80 || !validMethod(method) {
		return nil, errors.New("invalid bridge method")
	}
	b, e := json.Marshal(map[string]interface{}{"method": method, "params": args})
	if e != nil {
		return nil, e
	}
	r, e := http.NewRequestWithContext(ctx, "POST", c.url+"/v1/query", bytes.NewReader(b))
	if e != nil {
		return nil, e
	}
	r.Header.Set("Content-Type", "application/json")
	resp, e := c.http.Do(r)
	if e != nil {
		return nil, &Error{Message: e.Error()}
	}
	defer resp.Body.Close()
	body, e := io.ReadAll(io.LimitReader(resp.Body, maxBody+1))
	if e != nil {
		return nil, e
	}
	if len(body) > maxBody {
		return nil, errors.New("bridge response too large")
	}
	if resp.StatusCode != 200 {
		var payload struct {
			Error string `json:"error"`
		}
		if json.Unmarshal(body, &payload) != nil || payload.Error == "" {
			payload.Error = string(body)
		}
		return nil, &Error{StatusCode: resp.StatusCode, Message: payload.Error}
	}
	if !json.Valid(body) {
		return nil, errors.New("bridge returned invalid JSON")
	}
	return body, nil
}
func validMethod(s string) bool {
	for _, r := range s {
		if !(r >= 'a' && r <= 'z' || r >= '0' && r <= '9' || r == '_' || r == '.') {
			return false
		}
	}
	return true
}
