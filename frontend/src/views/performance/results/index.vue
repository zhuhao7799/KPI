<template>
  <div class="page p-2">
    <el-card shadow="never"><template #header><div class="heading"><div><h1>绩效结果查询</h1><p>仅展示已确认或已归档、且当前账号有权查看的单位结果</p></div><el-button :loading="loading" @click="refresh">查询</el-button></div></template>
      <div class="filters"><el-select v-model="cycleCode" placeholder="选择考核周期" @change="refresh"><el-option v-for="cycle in cycles" :key="cycle.code" :label="cycle.name" :value="cycle.code" /></el-select><el-select v-model="category" placeholder="全部单位类别" clearable @change="refresh"><el-option label="二级学院" value="COLLEGE" /><el-option label="职能部门" value="DEPARTMENT" /></el-select></div>
      <el-table :data="rows" v-loading="loading" empty-text="当前范围内暂无已确认结果">
        <el-table-column prop="rank" label="名次" width="90" /><el-table-column prop="unitName" label="考核单位" min-width="200" /><el-table-column prop="unitCategory" label="类别" width="140" /><el-table-column prop="totalScore" label="总分" width="120"><template #default="scope"><strong>{{ Number(scope.row.totalScore).toFixed(2) }}</strong></template></el-table-column><el-table-column prop="submissionId" label="结果编号" min-width="220" show-overflow-tooltip /><el-table-column label="操作" width="100"><template #default="scope"><el-button link type="primary" @click="archive(scope.row.submissionId)">归档</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup name="PerformanceResults" lang="ts">
import { onMounted, ref } from 'vue';
import { archiveSubmission, listCycles, listResults } from '@/api/kpi';
import type { AssessmentCycleVO, RankedResultVO } from '@/api/kpi/types';

const cycles = ref<AssessmentCycleVO[]>([]);
const cycleCode = ref('');
const category = ref('');
const rows = ref<RankedResultVO[]>([]);
const loading = ref(false);
async function refresh() {
  if (!cycleCode.value) { rows.value = []; return; }
  loading.value = true;
  try { rows.value = (await listResults(cycleCode.value, category.value || undefined)).data || []; }
  finally { loading.value = false; }
}
async function archive(submissionId: string) {
  await archiveSubmission(submissionId);
  await refresh();
}
onMounted(async () => {
  cycles.value = (await listCycles()).data || [];
  cycleCode.value = cycles.value[0]?.code || '';
  await refresh();
});
</script>
<style scoped>
.page .el-card{border-radius:12px}.heading{display:flex;justify-content:space-between;align-items:center}.heading h1{margin:0;color:#183b60;font-size:24px}.heading p{margin:8px 0 0;color:#789;font-size:13px}.filters{display:flex;gap:12px;margin-bottom:16px}.filters .el-select{width:220px}@media(max-width:600px){.filters{flex-direction:column}.filters .el-select{width:100%}}
</style>
