package com.project.artisancharcuterie.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ImmutableResourceException extends RuntimeException {

    public ImmutableResourceException(String resourceType, Long id) {
        super(String.format(
                "%s with id %s is immutable and cannot be modified or deleted once submitted.",
                resourceType, id
        ));
    }

}
