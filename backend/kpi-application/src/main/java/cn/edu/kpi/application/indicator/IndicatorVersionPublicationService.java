package cn.edu.kpi.application.indicator;

import cn.edu.kpi.domain.model.IndicatorVersion;
import cn.edu.kpi.domain.security.KpiAuthorization;
import cn.edu.kpi.domain.security.KpiPermission;
import cn.edu.kpi.domain.security.KpiRole;

import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/** Publishes one immutable indicator version; a code can never be republished. */
public final class IndicatorVersionPublicationService {
    private final IndicatorVersionRepository repository;
    private final Clock clock;

    public IndicatorVersionPublicationService(IndicatorVersionRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository); this.clock = Objects.requireNonNull(clock);
    }

    public void publish(IndicatorVersion version, String formulaVersion, int scoreScale, Set<KpiRole> roles) {
        KpiAuthorization.require(roles, KpiPermission.INDICATOR_VERSION_MANAGE);
        if (repository.findPublished(version.version()).isPresent()) throw new IllegalStateException("indicator version already published: " + version.version());
        if (formulaVersion == null || formulaVersion.isBlank()) throw new IllegalArgumentException("formula version is required");
        if (scoreScale < 0 || scoreScale > 8) throw new IllegalArgumentException("score scale must be in range 0..8");
        repository.publish(version, formulaVersion, scoreScale, clock.instant());
    }
}
