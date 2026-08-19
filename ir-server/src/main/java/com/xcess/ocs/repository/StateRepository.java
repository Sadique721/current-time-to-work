package com.xcess.ocs.repository;

import com.xcess.ocs.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {

    List<State> findByCountryIsoOrderByStateName(String countryIso);
}
