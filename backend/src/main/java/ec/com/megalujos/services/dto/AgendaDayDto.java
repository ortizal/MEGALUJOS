package ec.com.megalujos.services.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Día de la agenda con las órdenes programadas para esa fecha. */
public class AgendaDayDto {

    private LocalDate date;
    private List<ServiceOrderSummaryDto> orders = new ArrayList<>();

    public AgendaDayDto() {
    }

    public AgendaDayDto(LocalDate date) {
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<ServiceOrderSummaryDto> getOrders() {
        return orders;
    }

    public void setOrders(List<ServiceOrderSummaryDto> orders) {
        this.orders = orders;
    }
}
