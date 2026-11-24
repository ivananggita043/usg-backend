
package com.garmentqc.system.repository;

import com.garmentqc.system.model.AqlSamplingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AqlSamplingPlanRepository extends JpaRepository<AqlSamplingPlan, Long> {

    Optional<AqlSamplingPlan> findByLotSizeCode_IdAndAqlLevel_Id(Long lotSizeCodeId, Long aqlLevelId);
}
