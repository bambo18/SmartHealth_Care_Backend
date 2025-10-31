package com.smarthealthdog.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smarthealthdog.backend.domain.Condition;
import com.smarthealthdog.backend.domain.PetSpecies;

public interface ConditionRepository extends JpaRepository<Condition, Integer> {
    List<Condition> findBySpecies(PetSpecies species);
}
