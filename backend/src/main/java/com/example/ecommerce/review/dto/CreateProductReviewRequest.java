package com.example.ecommerce.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateProductReviewRequest {
    @Min(value = 1, message = "Puan en az 1 olmalı.")
    @Max(value = 5, message = "Puan en fazla 5 olmalı.")
    private int rating;

    @NotBlank(message = "Yorum metni zorunludur.")
    @Size(min = 10, max = 500, message = "Yorum 10 ile 500 karakter arasında olmalı.")
    private String comment;
}
