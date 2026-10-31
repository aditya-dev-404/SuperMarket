package com.aditya.supermarket.order.repository;

import com.aditya.supermarket.order.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    /** One page of a user's orders. Sub-orders are loaded by {@link #findAllDetailedByIdIn} (paging and collection fetch joins must not be mixed). */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @Query("""
        select o from CustomerOrder o
        join fetch o.subOrders s
        join fetch s.vendor
        where o.id in :ids
        """)
    List<Order> findAllDetailedByIdIn(@Param("ids") Collection<Long> ids);

    /** Someone else's order is simply "not found". */
    @Query("""
        select o from CustomerOrder o
        join fetch o.subOrders s
        join fetch s.vendor
        where o.id = :id and o.userId = :userId
        """)
    Optional<Order> findDetailedByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Same order, but locked until the transaction ends. Payment, cancellation and the vendor's status updates
     * all load the order this way, so two of them can never act on the same order at the same time
     * (for example a cancel racing a "shipped" update, which would return stock for a parcel that left).
     * The vendor is deliberately not fetched here, so a vendor row is never locked.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o from CustomerOrder o
        join fetch o.subOrders
        where o.id = :id and o.userId = :userId
        """)
    Optional<Order> findForUpdateByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * The vendor's side of the same lock: loads (and locks) the whole order that contains the vendor's sub-order,
     * so a status update cannot run at the same time as a customer cancel or a payment on that order.
     * Empty when the sub-order does not exist or belongs to another vendor.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o from CustomerOrder o
        join fetch o.subOrders
        where o.id = (select s.customerOrder.id from SubOrder s
                      where s.id = :subOrderId and s.vendor.id = :vendorId)
        """)
    Optional<Order> findForUpdateBySubOrderIdAndVendorId(@Param("subOrderId") Long subOrderId,
                                                         @Param("vendorId") Long vendorId);
}
