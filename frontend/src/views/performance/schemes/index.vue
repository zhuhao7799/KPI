<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header><div class="toolbar"><span>绩效方案管理</span><el-button type="primary" @click="dialog = true">新增方案</el-button></div></template>
      <el-table :data="schemes" border><el-table-column prop="code" label="方案编码" width="160" /><el-table-column prop="name" label="方案名称" /><el-table-column prop="category" label="适用组织" width="150" /><el-table-column prop="status" label="状态" width="120" /><el-table-column label="操作" width="280"><template #default="{ row }"><el-button link type="primary" @click="configure(row)">指标配置</el-button><template v-if="row.status === 'DRAFT'"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="success" @click="publish(row.code)">发布</el-button><el-button link type="danger" @click="remove(row.code)">删除</el-button></template></template></el-table-column></el-table>
      <el-empty v-if="!schemes.length" description="暂无绩效方案，请新增方案" />
    </el-card>
    <el-dialog v-model="dialog" title="新增绩效方案" width="560px"><el-form :model="form" label-width="110px"><el-form-item label="方案编码"><el-input v-model="form.schemeCode" placeholder="例如 COLLEGE-2026" /></el-form-item><el-form-item label="方案名称"><el-input v-model="form.schemeName" /></el-form-item><el-form-item label="适用组织"><el-select v-model="form.organizationCategory"><el-option label="二级学院" value="二级学院" /><el-option label="职能部门" value="职能部门" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template></el-dialog>
      <el-dialog v-model="indicatorDialog" :title="`指标配置：${activeScheme}`" width="760px"><el-alert :title="`当前权重合计：${weightTotal}%（发布要求 100%）`" :type="weightTotal === 100 ? 'success' : 'warning'" show-icon :closable="false" /><el-table :data="indicators" border><el-table-column prop="code" label="编码" width="150" /><el-table-column prop="name" label="名称" /><el-table-column prop="weight" label="权重" width="100" /><el-table-column prop="maximumScore" label="满分" width="100" /><el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" @click="editIndicator(row)">编辑</el-button><el-button link type="danger" @click="removeIndicator(row.code)">删除</el-button></template></el-table-column></el-table><el-form inline class="indicator-form"><el-input v-model="newIndicator.code" placeholder="编码" :disabled="editingIndicator" /><el-input v-model="newIndicator.name" placeholder="名称" /><el-input-number v-model="newIndicator.weight" :min="0.01" /><el-input-number v-model="newIndicator.maximumScore" :min="1" /><el-button type="primary" @click="saveIndicator">{{ editingIndicator ? '保存' : '新增' }}</el-button></el-form></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { addSchemeIndicator, createPerformanceScheme, deletePerformanceScheme, deleteSchemeIndicator, listPerformanceSchemes, listSchemeIndicators, publishPerformanceScheme, updatePerformanceScheme, updateSchemeIndicator, type PerformanceSchemeVO, type SchemeIndicatorVO } from '@/api/kpi';
const schemes = ref<PerformanceSchemeVO[]>([]);
const dialog = ref(false); const editing = ref(false); const form = reactive({ schemeCode: '', schemeName: '', organizationCategory: '二级学院' });
const indicatorDialog = ref(false); const activeScheme = ref(''); const indicators = ref<SchemeIndicatorVO[]>([]); const newIndicator = reactive<SchemeIndicatorVO>({ code: '', name: '', weight: 0, maximumScore: 100 });
const editingIndicator = ref(false);
const weightTotal = computed(() => Number(indicators.value.reduce((sum, item) => sum + Number(item.weight), 0).toFixed(2)));
async function load() { schemes.value = (await listPerformanceSchemes()).data || []; }
function edit(row: PerformanceSchemeVO) { editing.value = true; form.schemeCode = row.code; form.schemeName = row.name; form.organizationCategory = row.category; dialog.value = true; }
async function save() { if (!form.schemeCode || !form.schemeName) { ElMessage.warning('请填写方案编码和名称'); return; } if (editing.value) await updatePerformanceScheme(form.schemeCode, { name: form.schemeName, category: form.organizationCategory }); else await createPerformanceScheme({ code: form.schemeCode, name: form.schemeName, category: form.organizationCategory }); editing.value = false; dialog.value = false; ElMessage.success('方案已保存'); await load(); }
async function remove(code: string) { await deletePerformanceScheme(code); ElMessage.success('方案已删除'); await load(); }
async function configure(row: PerformanceSchemeVO) { activeScheme.value = row.code; indicators.value = (await listSchemeIndicators(row.code)).data || []; indicatorDialog.value = true; }
function editIndicator(row: SchemeIndicatorVO) { editingIndicator.value = true; newIndicator.code = row.code; newIndicator.name = row.name; newIndicator.weight = row.weight; newIndicator.maximumScore = row.maximumScore; }
async function saveIndicator() { if (!newIndicator.code || !newIndicator.name || newIndicator.weight <= 0) { ElMessage.warning('请完整填写指标项'); return; } if (editingIndicator.value) await updateSchemeIndicator(activeScheme.value, newIndicator.code, { name: newIndicator.name, weight: newIndicator.weight, maximumScore: newIndicator.maximumScore }); else { if (weightTotal.value + Number(newIndicator.weight) > 100) { ElMessage.warning('权重合计不能超过 100%'); return; } await addSchemeIndicator(activeScheme.value, newIndicator); } indicators.value = (await listSchemeIndicators(activeScheme.value)).data || []; editingIndicator.value = false; newIndicator.code = ''; newIndicator.name = ''; newIndicator.weight = 0; }
async function removeIndicator(code: string) { await deleteSchemeIndicator(activeScheme.value, code); indicators.value = (await listSchemeIndicators(activeScheme.value)).data || []; }
async function publish(code: string) { await publishPerformanceScheme(code); ElMessage.success('方案已发布'); await load(); }
onMounted(load);
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:600}</style>
