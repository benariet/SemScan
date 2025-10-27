package org.example.semscan.ui.teacher;

import org.example.semscan.data.api.ApiService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PresenterSeminarDisplayTest {

    @Test
    public void displayHelpers_prioritizeInstanceNameAndDescription() {
        ApiService.PresenterSeminarDto dto = createDto(
                "Custom Instance",
                "Catalog Seminar",
                "Presenter Description",
                "Catalog Description",
                buildSlots(new int[]{0, 3}, new int[]{7, 21}, new int[]{8, 22})
        );

        assertEquals("Custom Instance", dto.getDisplayTitle());
        assertEquals("Catalog Seminar", dto.getSeminarDisplayNameOrFallback());
        assertEquals("Presenter Description", dto.getDescriptionLine());
        assertEquals("Sun • 07:00–08:00, Wed • 21:00–22:00", dto.getNormalizedSlots());
    }

    @Test
    public void displayHelpers_fallbackToCatalogWhenInstanceMissing() {
        ApiService.PresenterSeminarDto dto = createDto(
                null,
                "Catalog Seminar",
                null,
                "Catalog Description",
                buildSlots(new int[]{1}, new int[]{10}, new int[]{12})
        );

        assertEquals("Catalog Seminar", dto.getDisplayTitle());
        assertEquals("Catalog Seminar", dto.getSeminarDisplayNameOrFallback());
        assertEquals("Catalog Description", dto.getDescriptionLine());
        assertEquals("Mon • 10:00–12:00", dto.getNormalizedSlots());
    }

    @Test
    public void descriptionPrefersPresenterText() {
        ApiService.PresenterSeminarDto dto = createDto(
                "Instance",
                "Catalog",
                "Presenter overrides",
                "Catalog source",
                buildSlots(new int[]{2}, new int[]{9}, new int[]{11})
        );

        assertEquals("Presenter overrides", dto.getDescriptionLine());
    }

    private ApiService.PresenterSeminarDto createDto(String instanceName,
                                                     String catalogName,
                                                     String presenterDescription,
                                                     String catalogDescription,
                                                     List<ApiService.PresenterSeminarSlotDto> slots) {
        ApiService.PresenterSeminarDto dto = new ApiService.PresenterSeminarDto();
        dto.seminarInstanceName = instanceName;
        dto.seminarDisplayName = catalogName;
        dto.tileDescription = presenterDescription;
        dto.seminarDescription = catalogDescription;
        dto.slots = slots;
        return dto;
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

