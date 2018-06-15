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
            StringBuilder sb = new StringBuilder("\n");

            // request
            if (request != null) {
                sb.append("request:");
                sb.append(request);
                sb.append(System.lineSeparator());
            }
            // response
            if (response != null) {
                sb.append("response:");
                sb.append(response);
                sb.append(System.lineSeparator());
            }
            // message
            if (message != null) {
                sb.append(message);
                sb.append(System.lineSeparator());
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