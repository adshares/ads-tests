package net.adshares.ads.qa.util;

public class AssertReason {

    interface AssertReasonBuilder {
        String build();

        AssertReasonBuilder req(String request);

        AssertReasonBuilder res(String response);

        AssertReasonBuilder msg(String msg);

    }

    private AssertReason() {
    }

    public static class Builder implements AssertReasonBuilder {

        private String request;
        private String response;
        private String message;

        public Builder() {}

        public String build() {
            StringBuilder sb = new StringBuilder();

            // request
            if (request != null) {
                sb.append(System.lineSeparator());
                sb.append("request:");
                sb.append(request);
            }
            // response
            if (response != null) {
                sb.append(System.lineSeparator());
                sb.append("response:");
                sb.append(response);
            }
            if (request != null || response != null) {
                sb.append(System.lineSeparator());
            }
            // message
            if (message != null) {
                sb.append(message);
            }

            return sb.toString();
        }

        public Builder req(String request) {
            this.request = request;
            return this;
        }

        public Builder res(String response) {
            this.response = response;
            return this;
        }

        public Builder msg(String message) {
            if (this.message == null) {
                this.message = message;
            } else {
                this.message = this.message.concat("\n").concat(message);
            }
            return this;
        }
    }
}