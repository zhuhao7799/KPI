<template>
  <div class="dashboard p-2">
    <section class="hero">
      <div><p>高校组织绩效考核</p><h1>绩效工作台</h1><span>年度考核周期、单位填报、材料审核与结果查询</span>
        <div class="actions"><el-button type="primary" size="large" @click="open('/performance/assessment')">进入单位填报</el-button><el-button size="large" plain @click="open('/performance/tasks')">办理审核待办</el-button><el-button size="large" plain @click="open('/performance/results')">查询考核结果</el-button></div>
      </div>
      <aside><small>当前周期</small><b>{{ currentCycle?.name || '尚未创建' }}</b><el-tag :type="currentCycle?.state === 'LOCKED' ? 'success' : 'info'" effect="dark">{{ currentCycle?.state || '未开始' }}</el-tag><span>{{ serviceStatus }}</span></aside>
    </section>
    <section class="metrics">
      <article v-for="metric in metrics" :key="metric.label"><el-icon :class="metric.tone"><component :is="metric.icon" /></el-icon><div><b>{{ metric.value }}</b><span>{{ metric.label }}</span><small>{{ metric.note }}</small></div></article>
    </section>
    <section class="content">
      <el-card shadow="never"><template #header><div class="heading"><div><p>ASSESSMENT UNITS</p><h2>当前考核单位</h2></div><el-button link type="primary" @click="refresh">刷新</el-button></div></template>
        <el-table :data="units" v-loading="loading" empty-text="当前账号暂无可访问的考核单位">
          <el-table-column prop="name" label="考核单位" min-width="200" /><el-table-column label="组织类别" min-width="130"><template #default="scope"><el-tag effect="plain" type="info">{{ scope.row.category }}</el-tag></template></el-table-column><el-table-column prop="id" label="单位标识" min-width="170" show-overflow-tooltip /><el-table-column label="操作" width="90"><template #default><el-button link type="primary" @click="open('/performance/assessment')">填报</el-button></template></el-table-column>
        </el-table>
      </el-card>
      <div class="aside"><el-card shadow="never"><template #header><div class="heading"><h2>我的审核待办</h2><el-tag type="danger" effect="light">{{ tasks.length }} 项</el-tag></div></template><div v-if="!tasks.length" class="empty">当前没有可办理的任务</div><div v-for="task in tasks.slice(0, 5)" :key="task.id" class="task"><span class="dot"></span><div><b>{{ task.type }} · {{ task.submissionId }}</b><p>等待当前账号审核办理</p></div></div><el-button v-if="tasks.length" link type="primary" @click="open('/performance/tasks')">查看全部待办</el-button></el-card>
        <el-card shadow="never"><template #header><div class="heading"><h2>规则提示</h2></div></template><ul><li>评分按已发布指标版本计算。</li><li>佐证材料只能在授权单位范围内下载。</li><li>结果确认后才进入排名查询。</li></ul></el-card></div>
    </section>
  </div>
</template>
<script setup name="Index" lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Bell, DocumentChecked, OfficeBuilding, Warning } from '@element-plus/icons-vue';
import { getCurrentKpiActor, listCycles, listOrganizationUnits, listResults, listTasks } from '@/api/kpi';
import type { ApprovalTaskVO, AssessmentCycleVO, OrganizationUnitVO } from '@/api/kpi/types';

const router = useRouter();
const cycles = ref<AssessmentCycleVO[]>([]);
const units = ref<OrganizationUnitVO[]>([]);
const tasks = ref<ApprovalTaskVO[]>([]);
const resultCount = ref(0);
const loading = ref(false);
const serviceStatus = ref('正在连接绩效服务…');
const currentCycle = computed(() => cycles.value.find(cycle => cycle.state === 'LOCKED') || cycles.value[0]);
const metrics = computed(() => [
  { label: '考核周期', value: String(cycles.value.length), note: '当前账号可查看', icon: DocumentChecked, tone: 'blue' },
  { label: '考核单位', value: String(units.value.length), note: '当前账号可访问', icon: OfficeBuilding, tone: 'amber' },
  { label: '我的审核待办', value: String(tasks.value.length), note: '待办理', icon: Bell, tone: 'green' },
  { label: '已确认结果', value: String(resultCount.value), note: '当前范围内', icon: Warning, tone: 'red' }
]);
const open = (path: string) => router.push(path);

async function refresh() {
  loading.value = true;
  try {
    const [actorResponse, cycleResponse] = await Promise.all([getCurrentKpiActor(), listCycles()]);
    const roles = actorResponse.data?.roles || [];
    cycles.value = cycleResponse.data || [];
    serviceStatus.value = '已连接绩效服务';
    if (currentCycle.value) {
      const cycleCode = currentCycle.value.code;
      const canReview = roles.some(role => ['SCHOOL_ADMIN', 'ASSESSMENT_OFFICE', 'REVIEWER', 'EVALUATOR'].includes(role));
      const canViewResults = roles.length > 0;
      const [unitResponse, taskResponse, resultResponse] = await Promise.allSettled([
        listOrganizationUnits(cycleCode), canReview ? listTasks() : Promise.resolve(null), canViewResults ? listResults(cycleCode) : Promise.resolve(null)
      ]);
      units.value = unitResponse.status === 'fulfilled' ? unitResponse.value.data || [] : [];
      tasks.value = taskResponse.status === 'fulfilled' ? taskResponse.value?.data || [] : [];
      resultCount.value = resultResponse.status === 'fulfilled' ? resultResponse.value?.data?.length || 0 : 0;
    } else { units.value = []; tasks.value = []; resultCount.value = 0; }
  } catch {
    serviceStatus.value = '绩效服务暂不可用';
    cycles.value = []; units.value = []; tasks.value = []; resultCount.value = 0;
  } finally { loading.value = false; }
}
onMounted(refresh);
</script>
<style lang="scss" scoped>
.dashboard{display:flex;flex-direction:column;gap:18px}.hero{display:grid;grid-template-columns:1.35fr .65fr;gap:30px;padding:34px 38px;border-radius:16px;background:linear-gradient(125deg,#12375f,#1b5878);color:#fff;box-shadow:0 10px 28px rgb(18 55 95 / 16%)}.hero p{margin:0 0 9px;color:#b9d3e7;font-size:13px;letter-spacing:.09em}.hero h1{margin:0;font-size:32px}.hero>div>span{display:block;margin-top:13px;color:#d8e7ef}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:22px}.hero aside{align-self:center;padding:22px;border:1px solid rgb(255 255 255 / 18%);border-radius:12px;background:rgb(255 255 255 / 9%)}.hero aside small,.hero aside span{color:#c9dce9;font-size:13px}.hero aside b{display:block;margin:7px 0 16px;font-size:20px}.hero aside span{display:block;margin-top:12px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.metrics article{display:flex;gap:13px;align-items:center;padding:18px;border:1px solid var(--el-border-color-lighter);border-radius:12px;background:var(--el-bg-color)}.metrics .el-icon{display:grid;width:42px;height:42px;place-items:center;border-radius:10px;font-size:20px}.blue{background:#e9f2fb;color:#3475af}.amber{background:#fcf3df;color:#b77817}.green{background:#e5f4f1;color:#16766c}.red{background:#fcebed;color:#b84a55}.metrics b,.metrics span,.metrics small{display:block}.metrics b{font-size:24px;color:#183b60}.metrics span{font-size:13px;color:#3e5368}.metrics small{font-size:11px;color:#8492a0}.content{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(300px,.55fr);gap:18px}.content .el-card{border-radius:12px}.heading{display:flex;align-items:center;justify-content:space-between}.heading p{margin:0;color:#7d8c9b;font-size:11px;font-weight:700;letter-spacing:.08em}.heading h2{margin:5px 0 0;color:#193d60;font-size:18px}.aside{display:flex;flex-direction:column;gap:18px}.task{display:flex;gap:11px;padding:11px 0;border-bottom:1px solid var(--el-border-color-lighter)}.task:last-child{border-bottom:0}.dot{width:8px;height:8px;margin-top:6px;border-radius:50%;background:#c58a2e}.task b{font-size:13px;color:#31475c}.task p{margin:5px 0 0;color:#8492a0;font-size:12px}.empty{color:#8492a0;font-size:13px}.aside ul{margin:0;padding-left:19px;color:#607284;font-size:13px;line-height:1.9}@media(max-width:1000px){.metrics{grid-template-columns:repeat(2,1fr)}.content,.hero{grid-template-columns:1fr}}@media(max-width:600px){.metrics{grid-template-columns:1fr}.hero{padding:24px}.hero h1{font-size:26px}}
</style>
