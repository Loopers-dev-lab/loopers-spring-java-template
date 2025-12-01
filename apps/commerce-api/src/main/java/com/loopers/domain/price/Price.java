package com.loopers.domain.price;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class Price {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

//    private String
}
