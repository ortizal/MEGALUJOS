package ec.com.megalujos.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceName;
    private final Object identifier;

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s con identificador [%s] no fue encontrado", resourceName, identifier));
        this.resourceName = resourceName;
        this.identifier = identifier;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Recurso";
        this.identifier = null;
    }

    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
