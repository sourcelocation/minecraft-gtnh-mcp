package bridge

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestCallPreservesBridgeError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, _ *http.Request) {
		response.WriteHeader(http.StatusServiceUnavailable)
		_, _ = response.Write([]byte(`{"error":"game thread timed out"}`))
	}))
	defer server.Close()

	err := newAt(server.URL).Call(context.Background(), "status", struct{}{}, &struct{}{})
	var bridgeError *Error
	if !errors.As(err, &bridgeError) || bridgeError.StatusCode != http.StatusServiceUnavailable || bridgeError.Message != "game thread timed out" {
		t.Fatalf("unexpected error: %#v", err)
	}
}

func TestCallDecodesResponse(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, _ *http.Request) {
		_, _ = response.Write([]byte(`{"ok":true}`))
	}))
	defer server.Close()

	var output struct {
		OK bool `json:"ok"`
	}
	if err := newAt(server.URL).Call(context.Background(), "status", struct{}{}, &output); err != nil || !output.OK {
		t.Fatalf("unexpected response: %#v, %v", output, err)
	}
}

func TestCallRawRejectsInvalidJSON(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, _ *http.Request) {
		_, _ = response.Write([]byte(`not json`))
	}))
	defer server.Close()

	if _, err := newAt(server.URL).CallRaw(context.Background(), "status", struct{}{}); err == nil {
		t.Fatal("expected invalid JSON error")
	}
}
