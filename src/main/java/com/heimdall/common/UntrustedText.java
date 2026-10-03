package com.heimdall.common;

/**
 * Wraps PR title/body/commit messages/file contents so they can never be
 * silently string-concatenated into a system prompt as instructions.
 * docs/02-domain-model.md, "Untrusted fields":
 *   <untrusted kind="pull_request_body">...raw...</untrusted>
 */
public record UntrustedText(String kind, String raw) {

    public String toDelimitedBlock() {
        // Escaping of the closing tag prevents a trivial injection break-out
        // where the untrusted content itself contains "</untrusted>".
        String escaped = raw == null ? "" : raw.replace("</untrusted>", "&lt;/untrusted&gt;");
        return "<untrusted kind=\"" + kind + "\">\n" + escaped + "\n</untrusted>";
    }
}
