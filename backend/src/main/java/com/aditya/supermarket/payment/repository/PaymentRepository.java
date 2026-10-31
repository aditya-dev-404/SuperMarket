package com.aditya.supermarket.payment.repository;

import com.aditya.supermarket.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
