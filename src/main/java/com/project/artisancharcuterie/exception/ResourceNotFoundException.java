package com.project.artisancharcuterie.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s with id %d not found", resourceType, id));
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s '%s' not found", resourceType, identifier));
    }

}
