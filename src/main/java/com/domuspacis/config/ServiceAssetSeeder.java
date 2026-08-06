package com.domuspacis.config;

import com.domuspacis.booking.domain.*;
import com.domuspacis.booking.infrastructure.ServiceAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Idempotent startup seeder for service assets (rooms, halls, gardens, retreats).
 *
 * <p>The seed data normally lives in the Flyway migration {@code V3__seed_service_assets.sql},
 * but Flyway is <b>disabled by default</b> in {@code application.yml}
 * ({@code flyway.enabled: ${FLYWAY_ENABLED:false}}). On environments where that flag was never
 * enabled (such as the deployed HF Space), the {@code service_assets} table was auto-created empty
 * by Hibernate {@code ddl-auto: update} — which is why the public Services/Rooms pages show
 * "No services currently available."
 *
 * <p>This runner backstops that gap: it only inserts when the table is empty, so it is safe to run
 * repeatedly and never duplicates existing rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceAssetSeeder implements CommandLineRunner {

    private final ServiceAssetRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("ServiceAssetSeeder: found {} existing assets, skipping seed.", repository.count());
            return;
        }
        List<ServiceAsset> seeds = buildSeedData();
        repository.saveAll(seeds);
        log.info("ServiceAssetSeeder: inserted {} service assets.", seeds.size());
    }

    private List<ServiceAsset> buildSeedData() {
        // ── ROOMS ─────────────────────────────────────────────────────────────
        Room single = Room.builder()
                .name("Standard Single Room")
                .description("A cosy, well-appointed room perfect for solo travellers and pilgrims seeking peaceful rest. Features a comfortable single bed, work desk, and en-suite bathroom.")
                .capacity(1).pricePerUnit(BigDecimal.valueOf(35000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("101").roomType(Room.RoomType.SINGLE).floor(1).build();
        Room standardDouble = Room.builder()
                .name("Standard Double Room")
                .description("Spacious and comfortable for couples or business travellers, with garden or courtyard views. Includes a double bed, seating area, and en-suite bathroom.")
                .capacity(2).pricePerUnit(BigDecimal.valueOf(50000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("201").roomType(Room.RoomType.DOUBLE).floor(2).build();
        Room twin = Room.builder()
                .name("Twin Room")
                .description("Ideal for colleagues or friends travelling together, offering two comfortable single beds, shared bathroom, and a small sitting area.")
                .capacity(2).pricePerUnit(BigDecimal.valueOf(45000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("202").roomType(Room.RoomType.DOUBLE).floor(2).build();
        Room deluxe = Room.builder()
                .name("Deluxe Double Room")
                .description("Premium accommodation with a king-size bed, panoramic views of the gardens, mini-fridge, and an upgraded en-suite with bathtub.")
                .capacity(2).pricePerUnit(BigDecimal.valueOf(75000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("301").roomType(Room.RoomType.DOUBLE).floor(3).build();
        Room familySuite = Room.builder()
                .name("Family Suite")
                .description("A spacious two-room suite with a king bed in the master bedroom and two single beds in the adjoining room. Perfect for families of up to 4.")
                .capacity(4).pricePerUnit(BigDecimal.valueOf(120000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("302").roomType(Room.RoomType.FAMILY).floor(3).build();
        Room executive = Room.builder()
                .name("Executive Suite")
                .description("Our most luxurious offering — a separate living area, premium furnishings, kitchenette, balcony with city views, and a spacious bathroom with jacuzzi.")
                .capacity(3).pricePerUnit(BigDecimal.valueOf(150000)).pricingUnit(PricingUnit.PER_NIGHT)
                .roomNumber("401").roomType(Room.RoomType.SUITE).floor(4).build();


        // ── CONFERENCE HALLS ──────────────────────────────────────────────────
        ConferenceHall boardroom = ConferenceHall.builder()
                .name("Boardroom")
                .description("Intimate boardroom setting ideal for executive meetings, board sessions, and small workshops. Features a 70\" Smart TV, video conferencing, whiteboard, and coffee service.")
                .capacity(20).pricePerUnit(BigDecimal.valueOf(200000)).pricingUnit(PricingUnit.PER_DAY)
                .hallCode("BR-01").projectorAvailable(true).audioSystemAvailable(true)
                .maxSeatingLayout(ConferenceHall.SeatingLayout.BOARDROOM).build();
        ConferenceHall seminar = ConferenceHall.builder()
                .name("Seminar Hall")
                .description("Versatile hall suitable for seminars, training sessions, and mid-sized conferences with flexible seating. Equipped with projector, PA system, podium, and air conditioning.")
                .capacity(80).pricePerUnit(BigDecimal.valueOf(400000)).pricingUnit(PricingUnit.PER_DAY)
                .hallCode("SH-01").projectorAvailable(true).audioSystemAvailable(true)
                .maxSeatingLayout(ConferenceHall.SeatingLayout.CLASSROOM).build();
        ConferenceHall mainHall = ConferenceHall.builder()
                .name("Main Conference Hall")
                .description("Our flagship conference facility for large plenary sessions, AGMs, and multi-track conferences. Features dual projectors, professional PA system, simultaneous translation booth, stage, and breakout rooms.")
                .capacity(200).pricePerUnit(BigDecimal.valueOf(800000)).pricingUnit(PricingUnit.PER_DAY)
                .hallCode("CH-01").projectorAvailable(true).audioSystemAvailable(true)
                .maxSeatingLayout(ConferenceHall.SeatingLayout.THEATRE).build();
        ConferenceHall banquet = ConferenceHall.builder()
                .name("Banquet Hall")
                .description("Elegant hall designed for gala dinners, award ceremonies, and formal banquets. Includes a dance floor, stage, catering kitchen, and premium lighting system.")
                .capacity(150).pricePerUnit(BigDecimal.valueOf(600000)).pricingUnit(PricingUnit.PER_EVENT)
                .hallCode("BH-01").projectorAvailable(true).audioSystemAvailable(true)
                .maxSeatingLayout(ConferenceHall.SeatingLayout.BANQUET).build();

        // ── WEDDING GARDENS ───────────────────────────────────────────────────
        WeddingGarden roseGarden = WeddingGarden.builder()
                .name("Rose Garden")
                .description("An intimate garden adorned with roses and hedgerows — perfect for elegant, smaller ceremonies. Includes a gazebo, seating for guests, and a dedicated bridal preparation room.")
                .capacity(150).pricePerUnit(BigDecimal.valueOf(800000)).pricingUnit(PricingUnit.PER_EVENT)
                .isIndoor(false).hasStage(true).build();
        WeddingGarden mainGarden = WeddingGarden.builder()
                .name("Main Wedding Garden")
                .description("Our flagship outdoor venue with manicured lawns, water features, a permanent stage, and a dedicated bridal suite. Can accommodate large wedding parties with catering included.")
                .capacity(350).pricePerUnit(BigDecimal.valueOf(1500000)).pricingUnit(PricingUnit.PER_EVENT)
                .isIndoor(false).hasStage(true).build();
        WeddingGarden chapelGarden = WeddingGarden.builder()
                .name("Indoor Chapel Garden")
                .description("A beautiful indoor-outdoor hybrid venue featuring a glass-roofed chapel surrounded by tropical plants. Ideal for rainy season weddings with the feel of an outdoor ceremony.")
                .capacity(200).pricePerUnit(BigDecimal.valueOf(1200000)).pricingUnit(PricingUnit.PER_EVENT)
                .isIndoor(true).hasStage(true).build();

        // ── RETREAT CENTERS ───────────────────────────────────────────────────
        RetreatCenter paxHouse = RetreatCenter.builder()
                .name("Pax Retreat House")
                .description("The largest retreat facility, ideal for religious communities, youth programmes, and extended spiritual exercises. Features dormitory-style accommodation, a chapel, dining hall, and meditation garden.")
                .capacity(80).pricePerUnit(BigDecimal.valueOf(25000)).pricingUnit(PricingUnit.PER_NIGHT)
                .numberOfBeds(80).includesChapel(true).includesCatering(true).build();
        RetreatCenter silentium = RetreatCenter.builder()
                .name("Silentium Lodge")
                .description("A quiet, contemplative retreat centre for individual or small-group silent retreats. Each guest has a private room with desk, access to the prayer garden, and simple meals provided.")
                .capacity(12).pricePerUnit(BigDecimal.valueOf(45000)).pricingUnit(PricingUnit.PER_NIGHT)
                .numberOfBeds(12).includesChapel(true).includesCatering(true).build();
        RetreatCenter sanctuary = RetreatCenter.builder()
                .name("Sanctuary Cabin")
                .description("A private self-contained cabin nestled in the wooded area of the property. Perfect for personal retreats, spiritual direction, or sabbatical rest. Includes kitchenette and private prayer corner.")
                .capacity(2).pricePerUnit(BigDecimal.valueOf(60000)).pricingUnit(PricingUnit.PER_NIGHT)
                .numberOfBeds(2).includesChapel(false).includesCatering(false).build();

        return List.of(
                single, standardDouble, twin, deluxe, familySuite, executive,
                boardroom, seminar, mainHall, banquet,
                roseGarden, mainGarden, chapelGarden,
                paxHouse, silentium, sanctuary
        );
    }
}
