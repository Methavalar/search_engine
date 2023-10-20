package searchengine.dto.response;

import lombok.Value;

@Value
public class ErrorResponse {
    boolean result;
    String error;
}
