package cn.edu.kpi.application.cycle;

import cn.edu.kpi.domain.model.AssessmentCycle;
import cn.edu.kpi.domain.model.OrganizationUnit;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Creates a period and freezes the assessed-unit scope before scoring begins. */
public final class CycleAdministrationService {
    private final AssessmentCycleRepository cycles;
    private final OrganizationUnitSnapshotRepository units;
    private final Clock clock;

    public CycleAdministrationService(AssessmentCycleRepository cycles, OrganizationUnitSnapshotRepository units, Clock clock) {
        this.cycles = Objects.requireNonNull(cycles);
        this.units = Objects.requireNonNull(units);
        this.clock = Objects.requireNonNull(clock);
    }

    public void create(CreateCycleCommand command) {
        KpiAuthorization.require(command.roles(), KpiPermission.CYCLE_MANAGE);
        if (cycles.findByCode(command.code()).isPresent()) throw new IllegalStateException("assessment cycle already exists: " + command.code());
        cycles.insert(new AssessmentCycle(command.code(), command.name(), command.startsOn(), command.endsOn(), AssessmentCycle.Status.DRAFT));
    }

    public void captureUnits(String cycleCode, List<OrganizationUnit> snapshots, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.CYCLE_MANAGE);
        var cycle = cycles.findByCode(cycleCode).orElseThrow(() -> new IllegalArgumentException("unknown assessment cycle: " + cycleCode));
        if (cycle.status() != AssessmentCycle.Status.DRAFT) throw new IllegalStateException("units can be changed only while cycle is draft");
        if (snapshots == null || snapshots.isEmpty()) throw new IllegalArgumentException("at least one assessment unit is required");
        units.replaceForDraftCycle(cycleCode, List.copyOf(snapshots), clock.instant());
    }

    public void lock(String cycleCode, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.CYCLE_MANAGE);
        var cycle = cycles.findByCode(cycleCode).orElseThrow(() -> new IllegalArgumentException("unknown assessment cycle: " + cycleCode));
        if (cycle.status() != AssessmentCycle.Status.DRAFT && cycle.status() != AssessmentCycle.Status.ACTIVE) {
            throw new IllegalStateException("only draft or active cycles can be locked");
        }
        cycles.updateStatus(cycleCode, AssessmentCycle.Status.LOCKED);
    }

    public record CreateCycleCommand(String code, String name, LocalDate startsOn, LocalDate endsOn, Set<KpiRole> roles) {
        public CreateCycleCommand { roles = Set.copyOf(roles); }
    }
}
