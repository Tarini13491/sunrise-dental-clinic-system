package com.sunrise.clinic.service;

import com.sunrise.clinic.dao.DentistDao;
import com.sunrise.clinic.exception.ConflictException;
import com.sunrise.clinic.exception.NotFoundException;
import com.sunrise.clinic.exception.ValidationException;
import com.sunrise.clinic.model.Dentist;
import com.sunrise.clinic.model.DentistStatus;
import com.sunrise.clinic.model.SessionUser;
import com.sunrise.clinic.security.AccessPolicy;
import com.sunrise.clinic.validation.FieldValidator;

import java.util.List;

public class DentistService {
    private final DentistDao dentists;

    public DentistService(DentistDao dentists) {
        this.dentists = dentists;
    }

    public List<Dentist> list(SessionUser actor) {
        AccessPolicy.requireUser(actor);
        if (actor.isAdmin()) {
            return dentists.findAll();
        }
        return dentists.findActive();
    }

    public List<Dentist> listActive(SessionUser actor) {
        AccessPolicy.requireUser(actor);
        return dentists.findActive();
    }

    public Dentist register(SessionUser actor, String fullName) {
        AccessPolicy.requireAdmin(actor);
        String name = FieldValidator.personName(fullName, "Dentist name");
        Dentist existing = dentists.findByName(name).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == DentistStatus.INACTIVE) {
                dentists.updateStatus(existing.getDentistId(), DentistStatus.ACTIVE);
                existing.setStatus(DentistStatus.ACTIVE);
                return existing;
            }
            throw new ConflictException("That dentist is already on the clinic list.");
        }
        Dentist dentist = new Dentist();
        dentist.setFullName(name);
        dentist.setStatus(DentistStatus.ACTIVE);
        dentist.setDentistId(dentists.insert(dentist));
        return dentist;
    }

    public Dentist remove(SessionUser actor, int dentistId) {
        AccessPolicy.requireAdmin(actor);
        Dentist dentist = dentists.findById(dentistId)
                .orElseThrow(() -> new NotFoundException("Dentist was not found."));
        dentists.updateStatus(dentist.getDentistId(), DentistStatus.INACTIVE);
        dentist.setStatus(DentistStatus.INACTIVE);
        return dentist;
    }

    public Dentist restore(SessionUser actor, int dentistId) {
        AccessPolicy.requireAdmin(actor);
        Dentist dentist = dentists.findById(dentistId)
                .orElseThrow(() -> new NotFoundException("Dentist was not found."));
        dentists.updateStatus(dentist.getDentistId(), DentistStatus.ACTIVE);
        dentist.setStatus(DentistStatus.ACTIVE);
        return dentist;
    }

    public Dentist requireActive(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("A dentist must be selected.");
        }
        String name = FieldValidator.personName(fullName, "Dentist name");
        return dentists.findByName(name)
                .filter(Dentist::isActive)
                .orElseThrow(() -> new ValidationException("Select a dentist from the clinic list."));
    }
}
