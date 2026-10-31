package com.aditya.supermarket.address.repository;

import com.aditya.supermarket.address.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

    /** The default address first, then the newest. */
    List<Address> findAllByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    /** Ownership check built into the lookup: someone else's address is simply "not found". */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    List<Address> findAllByUserIdAndDefaultAddressTrue(Long userId);

    /** The newest other address of the user, used to pick a new default after the default one is deleted. */
    Optional<Address> findFirstByUserIdAndIdNotOrderByIdDesc(Long userId, Long id);
}
