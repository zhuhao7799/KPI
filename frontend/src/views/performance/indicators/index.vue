<template>
  <div class="app-container indicator-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar"><span>指标体系管理</span><div><el-button v-if="versions.length > 1" @click="compareVisible = true">版本对比</el-button><el-button type="primary" @click="openCreate">新建版本</el-button></div></div>
      </template>
      <el-alert title="已发布版本会作为填报和评分的只读快照，发布后不允许直接修改。" type="info" show-icon :closable="false" />
      <div v-if="versions.length" class="version-tabs">
        <el-radio-group v-model="selectedVersion" @change="selectVersion">
          <el-radio-button v-for="item in versions" :key="item.version" :value="item.version">{{ item.version }} · {{ item.status }}</el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="selectedSummary?.status === 'DRAFT'" class="version-actions"><el-button type="primary" @click="openEdit">编辑草稿</el-button><el-button type="success" @click="publishDraftVersion">发布草稿</el-button><el-button type="danger" @click="removeDraft">删除草稿</el-button></div>
      <div v-else-if="selectedSummary?.status === 'PUBLISHED'" class="version-actions"><el-button type="primary" @click="cloneVersion">复制为新版本</el-button><el-button type="warning" @click="archiveVersion">归档版本</el-button><el-tag type="info">已发布版本只读</el-tag></div>
      <el-alert v-if="references.length" class="mt-4" type="warning" :closable="false" title="当前版本存在业务引用，不能删除或直接变更" />
      <el-table v-if="references.length" :data="references" border class="mt-4"><el-table-column prop="type" label="引用类型" width="180" /><el-table-column prop="code" label="引用对象" /><el-table-column prop="status" label="状态" width="120" /></el-table>
      <div v-if="version" class="filters"><el-input v-model="keyword" clearable placeholder="搜索编码或名称" /><el-select v-model="categoryFilter" clearable placeholder="筛选分类"><el-option v-for="option in kpi_indicator_category" :key="option.value" :label="option.label" :value="option.value" /></el-select></div>
      <el-table v-if="version" :data="filteredIndicators" border class="mt-4" @row-click="showDetail">
        <template #empty><el-empty description="暂无指标" /></template>
        <el-table-column prop="code" label="指标编码" width="180" />
        <el-table-column prop="name" label="指标名称" min-width="220" />
        <el-table-column prop="category" label="指标分类" width="140" />
        <el-table-column prop="indicatorType" label="指标性质" width="110" />
        <el-table-column prop="measurementUnit" label="单位" width="90" />
        <el-table-column prop="weight" label="权重" width="120"><template #default="{ row }">{{ row.weight }}%</template></el-table-column>
        <el-table-column prop="maximumScore" label="满分" width="120" />
        <el-table-column v-if="selectedSummary?.status === 'DRAFT'" label="排序" width="130"><template #default="{ row }"><el-button link @click.stop="moveIndicator(row, -1)">上移</el-button><el-button link @click.stop="moveIndicator(row, 1)">下移</el-button></template></el-table-column>
      </el-table>
      <el-empty v-else description="暂无指标版本" />
    </el-card>
    <el-drawer v-model="detailVisible" title="指标详情" size="420px"><el-descriptions v-if="detailItem" :column="1" border><el-descriptions-item label="指标编码">{{ detailItem.code }}</el-descriptions-item><el-descriptions-item label="指标名称">{{ detailItem.name }}</el-descriptions-item><el-descriptions-item label="指标分类">{{ detailItem.category }}</el-descriptions-item><el-descriptions-item label="指标性质">{{ detailItem.indicatorType }}</el-descriptions-item><el-descriptions-item label="计量单位">{{ detailItem.measurementUnit }}</el-descriptions-item><el-descriptions-item label="评分方式">{{ detailItem.scoringMethod }}</el-descriptions-item><el-descriptions-item label="权重">{{ detailItem.weight }}%</el-descriptions-item><el-descriptions-item label="满分">{{ detailItem.maximumScore }}</el-descriptions-item><el-descriptions-item label="佐证材料">{{ detailItem.evidenceRequired ? '必须' : '不要求' }}</el-descriptions-item><el-descriptions-item label="指标说明">{{ detailItem.description || '暂无' }}</el-descriptions-item></el-descriptions></el-drawer>
    <el-dialog v-model="compareVisible" title="指标版本差异" width="760px"><el-form inline><el-select v-model="compareFrom" placeholder="基准版本"><el-option v-for="item in versions" :key="`from-${item.version}`" :label="item.version" :value="item.version" /></el-select><el-select v-model="compareTo" placeholder="对比版本"><el-option v-for="item in versions" :key="`to-${item.version}`" :label="item.version" :value="item.version" /></el-select><el-button type="primary" @click="runCompare">比较</el-button></el-form><el-table :data="changes" border><el-table-column prop="type" label="变更类型" width="100" /><el-table-column prop="code" label="指标编码" /><el-table-column prop="fromName" label="基准版本" /><el-table-column prop="toName" label="对比版本" /></el-table></el-dialog>

    <el-dialog v-model="dialogVisible" title="发布指标版本" width="680px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="版本编码"><el-input v-model="form.version" placeholder="例如：2027-v1" /></el-form-item>
        <el-form-item label="评分公式"><el-input v-model="form.formulaVersion" /></el-form-item>
        <el-form-item label="评分精度"><el-input-number v-model="form.scoreScale" :min="0" :max="8" /></el-form-item>
        <el-form-item label="指标项">
          <div class="items">
            <div v-for="(item, index) in form.indicators" :key="index" class="item-row">
              <el-input v-model="item.code" placeholder="编码" />
              <el-input v-model="item.name" placeholder="名称" />
              <el-select v-model="item.category" placeholder="分类"><el-option v-for="option in kpi_indicator_category" :key="option.value" :label="option.label" :value="option.value" /></el-select>
              <el-select v-model="item.indicatorType" placeholder="性质"><el-option v-for="option in kpi_indicator_type" :key="option.value" :label="option.label" :value="option.value" /></el-select>
              <el-select v-model="item.measurementUnit" placeholder="单位"><el-option v-for="option in kpi_measurement_unit" :key="option.value" :label="option.label" :value="option.value" /></el-select>
              <el-input-number v-model="item.weight" :min="0.01" :max="100" :precision="2" />
              <el-input-number v-model="item.maximumScore" :min="1" :precision="2" />
              <el-select v-model="item.scoringMethod" placeholder="评分方式"><el-option v-for="option in kpi_scoring_method" :key="option.value" :label="option.label" :value="option.value" /></el-select>
              <el-checkbox v-model="item.evidenceRequired">需材料</el-checkbox>
              <el-button text type="danger" @click="form.indicators.splice(index, 1)">删除</el-button>
            </div>
            <el-button plain @click="form.indicators.push({ code: '', name: '', weight: 0, maximumScore: 100, category: '综合管理', indicatorType: '定量', measurementUnit: '分', scoringMethod: '加权评分', evidenceRequired: false, description: '' })">新增指标</el-button>
            <el-button plain @click="fileInput?.click()">导入 CSV</el-button><el-button plain @click="exportCsv">导出 CSV</el-button><input ref="fileInput" type="file" accept=".csv,text/csv" hidden @change="importCsv" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRefs } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { archiveIndicatorVersion, cloneIndicatorVersion, compareIndicatorVersions, deleteIndicatorDraft, getIndicatorVersion, getIndicatorVersionItems, listIndicatorReferences, listIndicatorVersions, publishIndicatorDraft, saveIndicatorDraft } from '@/api/kpi';
import { useDict } from '@/utils/dict';
import type { IndicatorVersionInput, IndicatorVersionSummary } from '@/api/kpi/types';

const version = ref<IndicatorVersionInput>();
const versions = ref<IndicatorVersionSummary[]>([]);
const selectedVersion = ref('2026-v1');
const selectedSummary = computed(() => versions.value.find(item => item.version === selectedVersion.value));
const dialogVisible = ref(false);
const saving = ref(false);
const form = reactive<IndicatorVersionInput>({ version: '', formulaVersion: 'weighted-score-v1', scoreScale: 4, indicators: [] });
const references = ref<Array<{ type: string; code: string; status: string }>>([]);
const detailVisible = ref(false); const detailItem = ref<IndicatorVersionInput['indicators'][number]>();
const fileInput = ref<HTMLInputElement>();
const keyword = ref(''); const categoryFilter = ref(''); const compareVisible = ref(false); const compareFrom = ref(''); const compareTo = ref(''); const changes = ref<Array<{ type: string; code: string; fromName?: string; toName?: string }>>([]);
const filteredIndicators = computed(() => (version.value?.indicators || []).filter(item => (!keyword.value || `${item.code}${item.name}`.toLowerCase().includes(keyword.value.toLowerCase())) && (!categoryFilter.value || item.category === categoryFilter.value)));
const { kpi_indicator_category, kpi_indicator_type, kpi_measurement_unit, kpi_scoring_method } = toRefs<any>(useDict('kpi_indicator_category', 'kpi_indicator_type', 'kpi_measurement_unit', 'kpi_scoring_method'));

async function load() {
  try {
    versions.value = (await listIndicatorVersions()).data || [];
    if (versions.value.length && !versions.value.some(item => item.version === selectedVersion.value)) selectedVersion.value = versions.value[0].version;
    await selectVersion(selectedVersion.value);
  } catch { version.value = undefined; }
}
async function selectVersion(code: string) { selectedVersion.value = code; const summary = versions.value.find(item => item.version === code); references.value = (await listIndicatorReferences(code)).data || []; version.value = summary?.status === 'PUBLISHED' ? (await getIndicatorVersion(code)).data : { version: code, formulaVersion: summary?.formulaVersion || 'weighted-score-v1', scoreScale: summary?.scoreScale || 4, indicators: (await getIndicatorVersionItems(code)).data || [] }; }
function showDetail(item: IndicatorVersionInput['indicators'][number]) { detailItem.value = item; detailVisible.value = true; }
function moveIndicator(item: IndicatorVersionInput['indicators'][number], delta: number) { if (!version.value || selectedSummary.value?.status !== 'DRAFT') return; const index = version.value.indicators.indexOf(item); const target = index + delta; if (target < 0 || target >= version.value.indicators.length) return; const rows = version.value.indicators; [rows[index], rows[target]] = [rows[target], rows[index]]; version.value = { ...version.value, indicators: [...rows] }; }
function openCreate() { form.version = ''; form.formulaVersion = 'weighted-score-v1'; form.scoreScale = 4; form.indicators = [{ code: '', name: '', weight: 0, maximumScore: 100, category: '综合管理', indicatorType: '定量', measurementUnit: '分', scoringMethod: '加权评分', evidenceRequired: false, description: '' }]; dialogVisible.value = true; }
function openEdit() { if (!version.value) return; form.version = version.value.version; form.formulaVersion = version.value.formulaVersion; form.scoreScale = version.value.scoreScale; form.indicators = version.value.indicators.map(item => ({ ...item })); dialogVisible.value = true; }
async function saveDraft() {
  if (!form.version || !form.indicators.length || form.indicators.some(item => !item.code || !item.name || item.weight <= 0)) { ElMessage.warning('请完整填写版本和指标项'); return; }
  saving.value = true;
  try { await saveIndicatorDraft(form); ElMessage.success('指标草稿已保存'); dialogVisible.value = false; await load(); } finally { saving.value = false; }
}
async function publishDraftVersion() { await publishIndicatorDraft(selectedVersion.value); ElMessage.success('指标版本发布成功'); await load(); }
async function removeDraft() { await deleteIndicatorDraft(selectedVersion.value); ElMessage.success('指标草稿已删除'); await load(); }
async function archiveVersion() { await ElMessageBox.confirm('归档后不能再被新方案引用，是否继续？', '归档确认'); await archiveIndicatorVersion(selectedVersion.value); ElMessage.success('版本已归档'); await load(); }
function exportCsv() { const header='编码,名称,分类,性质,单位,权重,满分,评分方式,需要材料,说明'; const rows=(form.indicators || []).map(item => [item.code,item.name,item.category,item.indicatorType,item.measurementUnit,item.weight,item.maximumScore,item.scoringMethod,item.evidenceRequired ? '是' : '否',item.description].map(value => `"${String(value ?? '').replace(/"/g,'""')}"`).join(',')); const blob=new Blob(['\ufeff'+[header,...rows].join('\n')], { type:'text/csv;charset=utf-8' }); const url=URL.createObjectURL(blob); const link=document.createElement('a'); link.href=url; link.download=`${form.version || 'indicators'}.csv`; link.click(); URL.revokeObjectURL(url); }
function importCsv(event: Event) { const input=event.target as HTMLInputElement; const file=input.files?.[0]; if(!file)return; const reader=new FileReader(); reader.onload=() => { const lines=String(reader.result || '').split(/\r?\n/).slice(1).filter(Boolean); form.indicators=lines.map(line => { const cells=line.split(',').map(cell => cell.replace(/^"|"$/g,'').replace(/""/g,'"')); return { code:cells[0] || '', name:cells[1] || '', category:cells[2] || '综合管理', indicatorType:cells[3] || '定量', measurementUnit:cells[4] || '分', weight:Number(cells[5] || 0), maximumScore:Number(cells[6] || 100), scoringMethod:cells[7] || '加权评分', evidenceRequired:cells[8] === '是', description:cells[9] || '' }; }); ElMessage.success(`已导入 ${form.indicators.length} 条指标`); input.value=''; }; reader.readAsText(file,'UTF-8'); }
async function cloneVersion() { const result = await ElMessageBox.prompt('请输入新版本编码，例如 2026-v2', '复制指标版本', { inputPattern: /^[A-Za-z0-9._-]+$/, inputErrorMessage: '只能包含字母、数字、点、下划线和短横线' }); await cloneIndicatorVersion(selectedVersion.value, result.value); ElMessage.success('已复制为草稿版本'); await load(); selectedVersion.value = result.value; await selectVersion(result.value); }
async function runCompare() { if (!compareFrom.value || !compareTo.value || compareFrom.value === compareTo.value) { ElMessage.warning('请选择两个不同版本'); return; } changes.value = (await compareIndicatorVersions(compareFrom.value, compareTo.value)).data || []; }
onMounted(load);
</script>

<style scoped>
.toolbar{display:flex;align-items:center;justify-content:space-between;font-size:18px;font-weight:600}.version-tabs{margin-top:16px}.version-actions{margin-top:16px}.filters{display:flex;gap:12px;margin-top:16px}.filters .el-input{width:260px}.filters .el-select{width:180px}.mt-4{margin-top:16px}.items{width:100%;overflow:auto}.item-row{display:grid;grid-template-columns:120px 160px 120px 100px 90px 100px 100px 130px 80px auto;gap:8px;margin-bottom:8px;min-width:1100px}
</style>
