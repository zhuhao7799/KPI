<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header><div class="toolbar"><span>考核周期管理</span><el-button type="primary" @click="dialog = true">新增周期</el-button></div></template>
      <el-table :data="cycles" border>
        <el-table-column prop="code" label="周期编码" width="140" />
        <el-table-column prop="name" label="周期名称" min-width="240" />
        <el-table-column prop="startsOn" label="开始日期" width="140" />
        <el-table-column prop="endsOn" label="结束日期" width="140" />
        <el-table-column prop="state" label="状态" width="120" />
        <el-table-column label="操作" width="280"><template #default="{ row }"><el-button link type="primary" @click="configure(row.code)">方案绑定</el-button><el-button link type="warning" @click="validate(row.code)">配置校验</el-button><el-button v-if="row.state !== 'LOCKED'" link type="primary" @click="lock(row.code)">锁定周期</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="新增考核周期" width="520px">
      <el-form :model="form" label-width="100px"><el-form-item label="周期编码"><el-input v-model="form.code" placeholder="例如 2027" /></el-form-item><el-form-item label="周期名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="form.startsOn" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.endsOn" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="schemeDialog" title="周期方案绑定" width="680px">
      <el-table :data="bindings" border><el-table-column prop="category" label="组织类别" width="140" /><el-table-column prop="schemeCode" label="方案编码" /><el-table-column prop="indicatorVersion" label="指标版本" /><el-table-column prop="status" label="状态" width="100" /></el-table>
      <el-form inline><el-select v-model="binding.category"><el-option label="二级学院" value="二级学院" /><el-option label="职能部门" value="职能部门" /></el-select><el-select v-model="binding.schemeCode" placeholder="选择绩效方案" clearable><el-option v-for="scheme in schemes" :key="scheme.code" :label="`${scheme.name} (${scheme.code})`" :value="scheme.code" /></el-select><el-select v-model="binding.indicatorVersion" placeholder="选择指标版本"><el-option v-for="version in versions" :key="version.version" :label="version.version" :value="version.version" /></el-select><el-button type="primary" @click="bind">绑定</el-button></el-form>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { bindCycleScheme, createCycle, listCycleSchemes, listCycles, listIndicatorVersions, listPerformanceSchemes, lockCycle, validateCycleSchemes, type CycleSchemeVO, type IndicatorVersionSummary, type PerformanceSchemeVO } from '@/api/kpi';
import type { AssessmentCycleVO } from '@/api/kpi/types';
const cycles = ref<AssessmentCycleVO[]>([]); const dialog = ref(false); const form = reactive<AssessmentCycleVO>({ code: '', name: '', startsOn: '', endsOn: '', state: 'DRAFT' });
const schemeDialog = ref(false); const activeCycle = ref(''); const bindings = ref<CycleSchemeVO[]>([]); const binding = reactive({ category: '二级学院', schemeCode: '', indicatorVersion: '2026-v1' });
const schemes = ref<PerformanceSchemeVO[]>([]); const versions = ref<IndicatorVersionSummary[]>([]);
async function load() { cycles.value = (await listCycles()).data || []; }
async function save() { await createCycle(form); ElMessage.success('周期创建成功'); dialog.value = false; await load(); }
async function lock(code: string) { await lockCycle(code); ElMessage.success('周期已锁定'); await load(); }
async function configure(code: string) { activeCycle.value = code; bindings.value = (await listCycleSchemes(code)).data || []; schemes.value = (await listPerformanceSchemes()).data || []; versions.value = (await listIndicatorVersions()).data || []; schemeDialog.value = true; }
async function bind() { await bindCycleScheme(activeCycle.value, binding); ElMessage.success('方案绑定成功'); bindings.value = (await listCycleSchemes(activeCycle.value)).data || []; }
async function validate(code: string) { const errors = (await validateCycleSchemes(code)).data || []; if (errors.length) ElMessage.warning(errors.join('；')); else ElMessage.success('周期配置完整，可以发布'); }
onMounted(load);
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:600}</style>
