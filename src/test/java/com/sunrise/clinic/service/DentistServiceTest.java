package com.sunrise.clinic.service;

import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.ForbiddenException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Dentist;
import com.sunrise.clinic.model.DentistStatus;
import com.sunrise.clinic.model.Role;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.support.InMemoryDentistDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DentistServiceTest {
    private DentistService dentistService;
    private final SessionUser admin = new SessionUser(1, "admin", "Clinic Administrator", Role.ADMIN);
    private final SessionUser staff = new SessionUser(2, "nimal", "Nimal Perera", Role.STAFF);

    @BeforeEach
    void setUp() {
        dentistService = new DentistService(new InMemoryDentistDao());
    }

    @Test
    void staffCannotRegisterDentists() {
        assertThrows(ForbiddenException.class, () -> dentistService.register(staff, "Dr Anika Perera"));
    }

    @Test
    void adminAddsDentistsAndStaffSeesOnlyActiveNames() {
        dentistService.register(admin, "Dr Anika Perera");
        Dentist removed = dentistService.register(admin, "Dr Saman Silva");
        dentistService.remove(admin, removed.getDentistId());
        assertEquals(2, dentistService.list(admin).size());
        assertEquals(1, dentistService.list(staff).size());
        assertEquals("Dr Anika Perera", dentistService.listActive(staff).get(0).getFullName());
    }

    @Test
    void rejectsADuplicateActiveDentist() {
        dentistService.register(admin, "Dr Anika Perera");
        assertThrows(ConflictException.class, () -> dentistService.register(admin, "Dr Anika Perera"));
    }

    @Test
    void restoringARemovedDentistPutsThemBackOnTheClinicList() {
        Dentist dentist = dentistService.register(admin, "Dr Anika Perera");
        dentistService.remove(admin, dentist.getDentistId());
        Dentist restored = dentistService.restore(admin, dentist.getDentistId());
        assertEquals(DentistStatus.ACTIVE, restored.getStatus());
        assertEquals(1, dentistService.listActive(staff).size());
    }

    @Test
    void addingARemovedDentistAgainReactivatesTheExistingRecord() {
        Dentist dentist = dentistService.register(admin, "Dr Anika Perera");
        dentistService.remove(admin, dentist.getDentistId());
        Dentist returned = dentistService.register(admin, "Dr Anika Perera");
        assertEquals(dentist.getDentistId(), returned.getDentistId());
        assertEquals(DentistStatus.ACTIVE, returned.getStatus());
    }

    @Test
    void requireActiveRejectsBlankAndRemovedDentists() {
        dentistService.register(admin, "Dr Anika Perera");
        ValidationException blank = assertThrows(ValidationException.class, () -> dentistService.requireActive(" "));
        assertEquals("A dentist must be selected.", blank.getMessage());
        Dentist dentist = dentistService.list(admin).get(0);
        dentistService.remove(admin, dentist.getDentistId());
        ValidationException removed = assertThrows(ValidationException.class, () -> dentistService.requireActive("Dr Anika Perera"));
        assertTrue(removed.getMessage().contains("clinic list"));
    }
}
