package cn.edu.kpi.ruoyi;

import cn.edu.kpi.application.security.KpiActor;
import cn.edu.kpi.application.security.KpiIdentityResolver;
import org.dromara.common.satoken.utils.LoginHelper;

import java.time.Clock;
import java.util.Objects;

/** Obtains the current authenticated RuoYi user and resolves its effective KPI roles. */
public final class KpiCurrentActor {
    private final KpiIdentityResolver identities;
    private final Clock clock;

    public KpiCurrentActor(KpiIdentityResolver identities, Clock kpiClock) {
        this.identities = Objects.requireNonNull(identities);
        this.clock = Objects.requireNonNull(kpiClock);
    }

    public KpiActor requireCurrent() {
        String userId = LoginHelper.getUserIdStr();
        if (userId == null || userId.isBlank()) throw new KpiUnauthenticatedException();
        return identities.resolve(userId, clock.instant());
    }
}
