package com.aditya.supermarket.order.repository;

import com.aditya.supermarket.order.entity.PaymentMethod;
import com.aditya.supermarket.order.entity.PaymentStatus;
import com.aditya.supermarket.order.entity.SubOrder;
import com.aditya.supermarket.order.entity.SubOrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Vendor-side reads. A vendor only ever sees orders that are really theirs to fulfil: cash-on-delivery
 * orders from the start, online orders once paid (or paid and later cancelled, so the vendor knows
 * not to ship). Unpaid and failed online orders never reach the vendor.
 */
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {

    List<PaymentStatus> CONFIRMED_ONLINE_PAYMENT_STATUSES = List.of(PaymentStatus.PAID, PaymentStatus.REFUND_PENDING);

    default Page<SubOrder> findVisibleByVendor(Long vendorId, Collection<SubOrderStatus> statuses, Pageable pageable) {
        return searchVisible(vendorId, statuses, PaymentMethod.COD, CONFIRMED_ONLINE_PAYMENT_STATUSES, pageable);
    }

    default Optional<SubOrder> findVisibleByIdAndVendor(Long id, Long vendorId) {
        return searchVisibleById(id, vendorId, PaymentMethod.COD, CONFIRMED_ONLINE_PAYMENT_STATUSES);
    }

    /** Fetching the order (a to-one join) keeps paging in SQL; items are batch-loaded for the page afterwards. */
    @Query(value = """
        select s from SubOrder s join fetch s.customerOrder o
        where s.vendor.id = :vendorId
          and s.status in :statuses
          and (o.paymentMethod = :cod or o.paymentStatus in :confirmed)
        """,
        countQuery = """
        select count(s) from SubOrder s join s.customerOrder o
        where s.vendor.id = :vendorId
          and s.status in :statuses
          and (o.paymentMethod = :cod or o.paymentStatus in :confirmed)
        """)
    Page<SubOrder> searchVisible(@Param("vendorId") Long vendorId,
                                 @Param("statuses") Collection<SubOrderStatus> statuses,
                                 @Param("cod") PaymentMethod cod,
                                 @Param("confirmed") Collection<PaymentStatus> confirmed,
                                 Pageable pageable);

    @Query("""
        select s from SubOrder s join fetch s.customerOrder o
        where s.id = :id
          and s.vendor.id = :vendorId
          and (o.paymentMethod = :cod or o.paymentStatus in :confirmed)
        """)
    Optional<SubOrder> searchVisibleById(@Param("id") Long id,
                                         @Param("vendorId") Long vendorId,
                                         @Param("cod") PaymentMethod cod,
                                         @Param("confirmed") Collection<PaymentStatus> confirmed);
}
