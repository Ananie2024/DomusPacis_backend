package com.domuspacis.booking.infrastructure;
import com.domuspacis.booking.domain.Booking;
import com.domuspacis.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    Page<Booking> findByServiceAssetId(UUID assetId, Pageable pageable);
    @Query("SELECT b FROM Booking b WHERE b.serviceAsset.id = :assetId AND b.status NOT IN ('CANCELLED','COMPLETED') AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn")
    List<Booking> findConflicting(@Param("assetId") UUID assetId, @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);
    @Query("SELECT b FROM Booking b WHERE b.serviceAsset.id = :assetId AND b.status NOT IN ('CANCELLED','COMPLETED') AND b.checkInDate < :checkOut AND b.checkOutDate > :checkIn AND b.id <> :excludeId")
    List<Booking> findConflictingExcluding(@Param("assetId") UUID assetId, @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut, @Param("excludeId") UUID excludeId);
    @Query("SELECT b FROM Booking b WHERE b.checkInDate = :date AND b.status = 'CONFIRMED'")
    List<Booking> findTodaysCheckIns(@Param("date") LocalDate date);
    @Query("SELECT b FROM Booking b WHERE b.checkOutDate = :date AND b.status = 'CHECKED_IN'")
    List<Booking> findTodaysCheckOuts(@Param("date") LocalDate date);
    long countByStatus(BookingStatus status);
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.checkInDate BETWEEN :from AND :to")
    long countBookingsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
