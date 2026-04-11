package com.project.artisancharcuterie.exception;

import com.project.artisancharcuterie.domain.enums.BatchStatus;
import com.project.artisancharcuterie.domain.enums.ChamberType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(String message) {
        super(message);
    }

    public IllegalTransitionException(BatchStatus currentStatus, ChamberType targetChamberType) {
        super(String.format(
                "Cannot transition a batch with status '%s' into a '%s' chamber. " +
                        "Review the required curing workflow sequence.",
                currentStatus.getDisplayName(),
                targetChamberType.getDisplayName()
        ));
    }

    public IllegalTransitionException(BatchStatus currentStatus) {
        super(String.format(
                "Batch with terminal status '%s' cannot be transitioned.",
                currentStatus.getDisplayName()
        ));
    }

}
