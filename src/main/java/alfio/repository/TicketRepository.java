/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.datamapper.Bind;
import alfio.datamapper.Query;
import alfio.datamapper.QueryRepository;
import alfio.datamapper.QueryType;
import alfio.model.FullTicketInfo;
import alfio.model.Ticket;

import java.util.List;

@QueryRepository
public interface TicketRepository {

	String CONFIRMED = "'ACQUIRED', 'CHECKED_IN', 'TO_BE_PAID'";

	@Query(type = QueryType.TEMPLATE, value = "insert into ticket (uuid, creation, category_id, event_id, status, original_price_cts, paid_price_cts)"
			+ "values(:uuid, :creation, :categoryId, :eventId, :status, :originalPrice, :paidPrice)")
	String bulkTicketInitialization();

	@Query("select id from ticket where status = 'FREE' and category_id = :categoryId and event_id = :eventId and tickets_reservation_id is null order by id limit :amount for update")
	List<Integer> selectTicketInCategoryForUpdate(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId,	@Bind("amount") int amount);

    @Query("select id from ticket where status = 'FREE' and category_id = :categoryId and event_id = :eventId and tickets_reservation_id is null order by id desc limit :amount for update")
    List<Integer> lockTicketsToInvalidate(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId,	@Bind("amount") int amount);

    @Query("select count(*) from ticket where status in ("+CONFIRMED+") and category_id = :categoryId and event_id = :eventId and full_name is not null and email_address is not null")
    Integer countAssignedTickets(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);

    @Query("select * from ticket where status in ('PENDING', 'ACQUIRED', 'TO_BE_PAID', 'CANCELLED', 'CHECKED_IN') and category_id = :categoryId and event_id = :eventId")
    List<Ticket> findAllModifiedTickets(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);

	@Query("select count(*) from ticket where status in ('ACQUIRED', 'CHECKED_IN', 'PENDING') and category_id = :categoryId and event_id = :eventId")
	Integer countConfirmedAndPendingTickets(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);
    
    @Query("select count(*) from ticket where status not in (" + CONFIRMED + ", 'INVALIDATED', 'CANCELLED')  and category_id = :categoryId and event_id = :eventId")
    Integer countNotSoldTickets(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);

	@Query("update ticket set tickets_reservation_id = :reservationId, status = 'PENDING', user_language = :userLanguage where id in (:reservedForUpdate)")
	int reserveTickets(@Bind("reservationId") String reservationId,	@Bind("reservedForUpdate") List<Integer> reservedForUpdate, @Bind("userLanguage") String userLanguage);
	
	@Query("update ticket set tickets_reservation_id = :reservationId, special_price_id_fk = :specialCodeId, user_language = :userLanguage, status = 'PENDING' where id = :ticketId")
	void reserveTicket(@Bind("reservationId")String transactionId, @Bind("ticketId") int ticketId, @Bind("specialCodeId") int specialCodeId, @Bind("userLanguage") String userLanguage);

	@Query("update ticket set status = :status where tickets_reservation_id = :reservationId")
	int updateTicketsStatusWithReservationId(@Bind("reservationId") String reservationId, @Bind("status") String status);
	
	@Query("update ticket set status = :status where uuid = :uuid")
	int updateTicketStatusWithUUID(@Bind("uuid") String uuid, @Bind("status") String status);

	@Query("update ticket set status = 'INVALIDATED' where id in (:ids)")
	int invalidateTickets(@Bind("ids") List<Integer> ids);

	@Query("update ticket set original_price_cts = :priceInCents, paid_price_cts = :priceInCents where event_id = :eventId and category_id = :categoryId")
	int updateTicketPrice(@Bind("categoryId") int categoryId, @Bind("eventId") int eventId, @Bind("priceInCents") int priceInCents);

	@Query("update ticket set status = 'FREE', tickets_reservation_id = null, special_price_id_fk = null where status in ('PENDING', 'OFFLINE_PAYMENT') "
			+ " and tickets_reservation_id in (:reservationIds)")
	int freeFromReservation(@Bind("reservationIds") List<String> reservationIds);

	@Query("select * from ticket where tickets_reservation_id = :reservationId order by category_id asc, uuid asc")
	List<Ticket> findTicketsInReservation(@Bind("reservationId") String reservationId);

    @Query("select count(*) from ticket where tickets_reservation_id = :reservationId")
    Integer countTicketsInReservation(@Bind("reservationId") String reservationId);
	
	@Query("select * from ticket where uuid = :uuid")
	Ticket findByUUID(@Bind("uuid") String uuid);

	@Query("update ticket set email_address = :email, full_name = :fullName where uuid = :ticketIdentifier")
	int updateTicketOwner(@Bind("ticketIdentifier") String ticketIdentifier, @Bind("email") String email, @Bind("fullName") String fullName);

	@Query("update ticket set locked_assignment = :lockedAssignment where id = :id and category_id = :categoryId")
	int toggleTicketLocking(@Bind("id") int ticketId, @Bind("categoryId") int categoryId, @Bind("lockedAssignment") boolean locked);
	
	@Query("update ticket set job_title = :jobTitle, company = :company, phone_number = :phoneNumber, address = :address, country = :country, tshirt_size = :tShirtSize, notes = :notes, user_language = :userLanguage where uuid = :ticketIdentifier")
	int updateOptionalTicketInfo(@Bind("ticketIdentifier") String ticketIdentifier,
								 @Bind("jobTitle") String jobTitle,
								 @Bind("company") String company,
								 @Bind("phoneNumber") String phoneNumber,
								 @Bind("address") String address,
								 @Bind("country") String country,
								 @Bind("tShirtSize") String tShirtSize,
								 @Bind("notes") String notes,
								 @Bind("userLanguage") String userLanguage);

	@Query("select * from ticket where id = :id and category_id = :categoryId")
	Ticket findById(@Bind("id") int ticketId, @Bind("categoryId") int categoryId);

	@Query("select * from ticket where special_price_id_fk = :specialPriceId")
	Ticket findBySpecialPriceId(@Bind("specialPriceId") int specialPriceId);

	@Query("update ticket set category_id = :targetCategoryId, original_price_cts = :originalPrice, paid_price_cts = :paidPrice where id in (:ticketIds)")
	int moveToAnotherCategory(@Bind("ticketIds") List<Integer> ticketIds, @Bind("targetCategoryId") int targetCategoryId, @Bind("originalPrice") int originalPrice, @Bind("paidPrice") int paidPrice);

	@Query("select * from ticket where category_id in (:categories) and status = 'PENDING'")
	List<Ticket> findPendingTicketsInCategories(@Bind("categories") List<Integer> categories);
	
	@Query("select " +
            " t.id t_id, t.uuid t_uuid, t.creation t_creation, t.category_id t_category_id, t.status t_status, t.event_id t_event_id," +
            " t.original_price_cts t_original_price_cts, t.paid_price_cts t_paid_price_cts, t.tickets_reservation_id t_tickets_reservation_id," +
            " t.full_name t_full_name, t.email_address t_email_address, t.locked_assignment t_locked_assignment, t.job_title t_job_title, t.company t_company," +
            " t.phone_number t_phone_number, t.address t_address, t.country t_country, t.tshirt_size t_tshirt_size," +
            " t.notes t_notes, t.user_language t_user_language," +
            " tr.id tr_id, tr.validity tr_validity, tr.status tr_status, tr.full_name tr_full_name, tr.email_address tr_email_address, tr.billing_address tr_billing_address," +
            " tr.confirmation_ts tr_confirmation_ts, tr.latest_reminder_ts tr_latest_reminder_ts, tr.payment_method tr_payment_method, tr.offline_payment_reminder_sent tr_offline_payment_reminder_sent, tr.promo_code_id_fk tr_promo_code_id_fk," +
            " tc.id tc_id, tc.inception tc_inception, tc.expiration tc_expiration, tc.max_tickets tc_max_tickets, tc.name tc_name, tc.description tc_description, tc.price_cts tc_price_cts, tc.access_restricted tc_access_restricted, tc.tc_status tc_tc_status, tc.event_id tc_event_id" +
            " from ticket t " +
            " inner join tickets_reservation tr on t.tickets_reservation_id = tr.id " +
            " inner join ticket_category tc on t.category_id = tc.id " +
            " where t.event_id = :eventId and t.full_name is not null and t.email_address is not null")
	List<FullTicketInfo> findAllFullTicketInfoAssignedByEventId(@Bind("eventId") int eventId);

    @Query("select a.*, b.confirmation_ts from ticket a, tickets_reservation b where a.event_id = :eventId and a.status in(" + CONFIRMED + ") and a.tickets_reservation_id = b.id order by b.confirmation_ts")
    List<Ticket> findAllConfirmed(@Bind("eventId") int eventId);

    @Query("select * from ticket where event_id = :eventId and status in(" + CONFIRMED + ") and category_id = :categoryId")
    List<Ticket> findConfirmedByCategoryId(@Bind("eventId") int eventId, @Bind("categoryId") int categoryId);

    @Query("select count(*) from ticket where event_id = :eventId and status in(" + CONFIRMED + ") and full_name is not null and email_address is not null")
	Integer countAllAssigned(@Bind("eventId") int eventId);

    @Query("select distinct tickets_reservation_id from ticket where event_id = :eventId and status in('ACQUIRED', 'TO_BE_PAID') and (full_name is null or email_address is null)")
    List<String> findAllReservationsConfirmedButNotAssigned(@Bind("eventId") int eventId);

}
