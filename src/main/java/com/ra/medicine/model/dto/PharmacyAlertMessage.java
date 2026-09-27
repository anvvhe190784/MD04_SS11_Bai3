package com.ra.medicine.model.dto;

import java.io.Serializable;

public record PharmacyAlertMessage(
        String type,
        String message
) implements Serializable {
}
