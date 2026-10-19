<template>
  <div class="assessment-page p-2">
    <el-card shadow="never" class="heading">
      <div><p>年度组织绩效考核</p><h1>单位绩效填报</h1><span>{{ statusText }}</span></div>
      <div class="actions"><el-button :loading="busy" :disabled="!ready || !!submissionId" @click="preview">预览评分</el-button><el-button type="primary" :loading="busy" :disabled="!ready || !!submissionId" @click="saveDraft">保存草稿</el-button><el-button type="success" :loading="busy" :disabled="!submissionId || submitted" @click="submit">提交审核</el-button></div>
    </el-card>
    <el-alert v-if="loadError" :title="loadError" type="warning" show-icon :closable="false" />
    <div class="form-grid">
      <el-card shadow="never"><template #header><h2>考核对象</h2></template>
        <el-form label-position="top">
          <el-form-item label="考核周期"><el-select v-model="cycleCode" :disabled="!!submissionId" placeholder="选择已锁定周期" @change="loadCycle"><el-option v-for="cycle in cycles" :key="cycle.code" :label="cycle.name" :value="cycle.code" :disabled="cycle.state !== 'LOCKED'" /></el-select></el-form-item>
          <el-form-item label="单位"><el-select v-model="unitId" :disabled="!!submissionId" placeholder="选择授权单位"><el-option v-for="unit in units" :key="unit.id" :label="unit.name" :value="unit.id" /></el-select></el-form-item>
          <el-form-item label="指标版本"><el-input v-model="versionCode" :disabled="!!submissionId" placeholder="已发布版本代码" @change="loadIndicators" /></el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never"><template #header><h2>评分汇总</h2></template>
        <strong class="total-score">{{ displayedScore.toFixed(2) }} 分</strong>
        <p>评分由服务端按已发布指标权重计算。保存草稿后，可逐项上传佐证材料。</p>
        <el-tag v-if="submissionId" :type="submitted ? 'warning' : 'success'">{{ submitted ? '已提交' : '草稿' }}编号：{{ submissionId }}</el-tag>
        <el-tag v-else type="info">尚未保存</el-tag>
      </el-card>
    </div>
    <el-card shadow="never" class="indicator-card">
      <template #header><div class="card-title"><h2>指标自评</h2><span>权重和满分来自已发布版本</span></div></template>
      <el-table :data="indicators" border empty-text="请先选择周期与已发布指标版本">
        <el-table-column prop="name" label="考核指标" min-width="200" />
        <el-table-column prop="weight" label="权重" width="100"><template #default="scope">{{ scope.row.weight }}%</template></el-table-column>
        <el-table-column label="自评分" width="180"><template #default="scope"><el-input-number v-model="scope.row.score" :disabled="!!submissionId" :min="0" :max="scope.row.maximumScore" :precision="2" controls-position="right" /></template></el-table-column>
        <el-table-column prop="maximumScore" label="满分" width="100" />
        <el-table-column label="材料佐证" min-width="220"><template #default="scope"><label v-if="submissionId && !submitted" class="upload-link">上传材料<input type="file" hidden @change="uploadMaterial($event, scope.row.code)" /></label><span v-else-if="!submissionId" class="muted">先保存草稿</span><div v-for="item in uploaded[scope.row.code] || []" :key="item.id" class="file-name">{{ item.filename }}</div></template></el-table-column>
        <el-table-column label="加权得分" width="130"><template #default="scope"><strong class="weighted">{{ ((scope.row.score / scope.row.maximumScore) * scope.row.weight).toFixed(2) }}</strong></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup name="PerformanceAssessment" lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getIndicatorVersion, listCycles, listOrganizationUnits, previewScore, saveSubmission, transitionSubmission, uploadSubmissionEvidence } from '@/api/kpi';
import type { AssessmentCycleVO, EvidenceUploadVO, OrganizationUnitVO, ScorePreviewRequest } from '@/api/kpi/types';

interface IndicatorRow { code: string; name: string; weight: number; maximumScore: number; score: number; }
const cycles = ref<AssessmentCycleVO[]>([]);
const units = ref<OrganizationUnitVO[]>([]);
const indicators = ref<IndicatorRow[]>([]);
const cycleCode = ref('');
const unitId = ref('');
const versionCode = ref('2026-v1');
const submissionId = ref('');
const submitted = ref(false);
const uploaded = ref<Record<string, EvidenceUploadVO[]>>({});
const serverScore = ref<number | null>(null);
const loadError = ref('');
const busy = ref(false);
const ready = computed(() => Boolean(cycleCode.value && unitId.value && versionCode.value && indicators.value.length));
const displayedScore = computed(() => serverScore.value ?? indicators.value.reduce((sum, row) => sum + row.score / row.maximumScore * row.weight, 0));
const statusText = computed(() => submitted.value ? '已提交，等待考核办指派审核' : submissionId.value ? '草稿已保存，可上传材料后提交审核' : '请选择考核周期、单位和已发布指标版本');

async function loadCycle() {
  units.value = [];
  unitId.value = '';
  submissionId.value = '';
  if (!cycleCode.value) return;
  const response = await listOrganizationUnits(cycleCode.value);
  units.value = response.data || [];
  unitId.value = units.value[0]?.id || '';
}

async function loadIndicators() {
  indicators.value = [];
  serverScore.value = null;
  if (!versionCode.value) return;
  try {
    const response = await getIndicatorVersion(versionCode.value);
    indicators.value = (response.data?.indicators || []).map(item => ({ code: item.code, name: item.name, weight: item.weight, maximumScore: item.maximumScore, score: 0 }));
    loadError.value = '';
  } catch {
    loadError.value = `指标版本 ${versionCode.value} 尚未发布或绩效服务不可用。`;
  }
}

function payload(): ScorePreviewRequest {
  return { cycleCode: cycleCode.value, unitId: unitId.value, indicatorVersion: versionCode.value,
    scores: indicators.value.map(row => ({ indicatorCode: row.code, rawScore: row.score, evidenceIds: [] })) };
}

async function preview() {
  if (!ready.value) return;
  busy.value = true;
  try {
    const response = await previewScore(payload());
    serverScore.value = Number(response.data.totalScore);
    ElMessage.success(`服务端评分：${serverScore.value.toFixed(2)} 分`);
  } finally { busy.value = false; }
}

async function saveDraft() {
  if (!ready.value) return;
  busy.value = true;
  try {
    const response = await saveSubmission(payload());
    submissionId.value = response.data.id;
    serverScore.value = Number(response.data.totalScore);
    ElMessage.success('草稿已保存');
  } finally { busy.value = false; }
}

async function submit() {
  if (!submissionId.value) return;
  busy.value = true;
  try {
    await transitionSubmission(submissionId.value, 'SUBMITTED');
    ElMessage.success('已提交，等待考核办指派审核');
    submitted.value = true;
  } finally { busy.value = false; }
}

async function uploadMaterial(event: Event, indicatorCode: string) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file || !submissionId.value) return;
  if (file.size > 20 * 1024 * 1024) { ElMessage.warning('单份材料不能超过 20 MiB'); input.value = ''; return; }
  busy.value = true;
  try {
    const response = await uploadSubmissionEvidence(submissionId.value, indicatorCode, file);
    uploaded.value[indicatorCode] = [...(uploaded.value[indicatorCode] || []), response.data];
    ElMessage.success('材料已上传并关联到指标');
  } finally { busy.value = false; input.value = ''; }
}

onMounted(async () => {
  try {
    const response = await listCycles();
    cycles.value = response.data || [];
    cycleCode.value = cycles.value.find(cycle => cycle.state === 'LOCKED')?.code || '';
    await Promise.all([loadCycle(), loadIndicators()]);
    if (!cycleCode.value) loadError.value = '当前没有已锁定的考核周期，请联系考核办。';
  } catch {
    loadError.value = '绩效服务暂不可用，请检查后端服务和登录状态。';
  }
});
</script>
<style lang="scss" scoped>
.assessment-page{display:flex;flex-direction:column;gap:18px}.heading,.form-grid :deep(.el-card),.indicator-card{border-radius:12px}.heading :deep(.el-card__body){display:flex;align-items:center;justify-content:space-between;gap:18px;border-left:4px solid #1d6f68}.heading p{margin:0;color:#6e8193;font-size:13px}.heading h1{margin:6px 0;color:#183b60;font-size:24px}.heading span{color:#8291a0;font-size:13px}.actions{display:flex;flex-wrap:wrap;gap:8px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.form-grid h2,.card-title h2{margin:0;color:#193d60;font-size:17px}.form-grid .el-select{width:100%}.form-grid p{color:#63768a;font-size:13px;line-height:1.7}.total-score{display:block;margin:12px 0;color:#1d6f68;font-size:30px}.card-title{display:flex;justify-content:space-between;align-items:center}.card-title span{color:#81909e;font-size:12px}.weighted{color:#1d6f68}.upload-link{color:var(--el-color-primary);cursor:pointer}.muted{color:#9aa8b4}.file-name{margin-top:4px;color:#607284;font-size:12px}@media(max-width:800px){.form-grid{grid-template-columns:1fr}.heading :deep(.el-card__body){align-items:flex-start;flex-direction:column}}
</style>
