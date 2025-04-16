package org.iesvdm.payment.paypal.repository;


import org.iesvdm.payment.paypal.model.Order;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByPaypalOrderId(String paypalOrderId);

}
