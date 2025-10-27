package org.example.semscan.ui.teacher;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.example.semscan.data.api.ApiService;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PresenterSeminarDisplayInstrumentedTest {

    @Test
    public void displayMethods_respectInstanceNameAndDescriptionPriority() {
        ApiService.PresenterSeminarDto dto = new ApiService.PresenterSeminarDto();
        dto.seminarInstanceName = "Custom Instance";
        dto.seminarDisplayName = "Catalog Seminar";
        dto.tileDescription = "Presenter description";
        dto.seminarDescription = "Catalog description";
        dto.slots = buildSlots(new int[]{0, 3}, new int[]{7, 21}, new int[]{8, 22});

        assertEquals("Custom Instance", dto.getDisplayTitle());
        assertEquals("Catalog Seminar", dto.getSeminarDisplayNameOrFallback());
        assertEquals("Presenter description", dto.getDescriptionLine());
        assertEquals("Sun • 07:00–08:00, Wed • 21:00–22:00", dto.getNormalizedSlots());
    }

    @Test
    public void displayMethods_fallbackWhenInstanceNameMissing() {
        ApiService.PresenterSeminarDto dto = new ApiService.PresenterSeminarDto();
        dto.seminarInstanceName = null;
        dto.seminarDisplayName = "Catalog Seminar";
        dto.seminarDescription = "Catalog description";
        dto.tileDescription = null;
        dto.slots = buildSlots(new int[]{1}, new int[]{10}, new int[]{12});

        assertEquals("Catalog Seminar", dto.getDisplayTitle());
        assertEquals("Catalog Seminar", dto.getSeminarDisplayNameOrFallback());
        assertEquals("Catalog description", dto.getDescriptionLine());
        assertEquals("Mon • 10:00–12:00", dto.getNormalizedSlots());
    }

    @Test
    public void description_prefersPresenterTextOverCatalog() {
        ApiService.PresenterSeminarDto dto = new ApiService.PresenterSeminarDto();
        dto.seminarInstanceName = "Instance";
        dto.seminarDisplayName = "Catalog";
        dto.tileDescription = "Presenter overrides";
        dto.seminarDescription = "Catalog source";
        dto.slots = buildSlots(new int[]{2}, new int[]{9}, new int[]{11});

        assertEquals("Presenter overrides", dto.getDescriptionLine());
    }

    private List<ApiService.PresenterSeminarSlotDto> buildSlots(int[] weekdays, int[] startHours, int[] endHours) {
        List<ApiService.PresenterSeminarSlotDto> slots = new ArrayList<>();
        for (int i = 0; i < weekdays.length; i++) {
            ApiService.PresenterSeminarSlotDto slot = new ApiService.PresenterSeminarSlotDto();
            slot.weekday = weekdays[i];
            slot.startHour = startHours[i];
            slot.endHour = endHours[i];
            slots.add(slot);
        }
        return slots;
    }
}

