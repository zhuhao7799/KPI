<template>
  <div class="app-container indicator-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar"><span>指标体系管理</span><el-button type="primary" @click="openCreate">发布新版本</el-button></div>
      </template>
      <el-alert title="已发布版本会作为填报和评分的只读快照，发布后不允许直接修改。" type="info" show-icon :closable="false" />
      <div v-if="versions.length" class="version-tabs">
        <el-radio-group v-model="selectedVersion" @change="selectVersion">
          <el-radio-button v-for="item in versions" :key="item.version" :value="item.version">{{ item.version }} · {{ item.status }}</el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="selectedSummary?.status === 'DRAFT'" class="version-actions"><el-button type="primary" @click="openEdit">编辑草稿</el-button><el-button type="success" @click="publishDraftVersion">发布草稿</el-button><el-button type="danger" @click="removeDraft">删除草稿</el-button></div>
      <div v-else-if="selectedSummary?.status === 'PUBLISHED'" class="version-actions"><el-button type="primary" @click="cloneVersion">复制为新版本</el-button><el-tag type="info">已发布版本只读</el-tag></div>
      <el-alert v-if="references.length" class="mt-4" type="warning" :closable="false" title="当前版本存在业务引用，不能删除或直接变更" />
      <el-table v-if="references.length" :data="references" border class="mt-4"><el-table-column prop="type" label="引用类型" width="180" /><el-table-column prop="code" label="引用对象" /><el-table-column prop="status" label="状态" width="120" /></el-table>
      <el-table v-if="version" :data="version.indicators" border class="mt-4">
        <el-table-column prop="code" label="指标编码" width="180" />
        <el-table-column prop="name" label="指标名称" min-width="220" />
        <el-table-column prop="weight" label="权重" width="120"><template #default="{ row }">{{ row.weight }}%</template></el-table-column>
        <el-table-column prop="maximumScore" label="满分" width="120" />
      </el-table>
      <el-empty v-else description="暂无指标版本" />
    </el-card>

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
              <el-input-number v-model="item.weight" :min="0.01" :max="100" :precision="2" />
              <el-input-number v-model="item.maximumScore" :min="1" :precision="2" />
              <el-button text type="danger" @click="form.indicators.splice(index, 1)">删除</el-button>
            </div>
            <el-button plain @click="form.indicators.push({ code: '', name: '', weight: 0, maximumScore: 100 })">新增指标</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { cloneIndicatorVersion, deleteIndicatorDraft, getIndicatorVersion, getIndicatorVersionItems, listIndicatorReferences, listIndicatorVersions, publishIndicatorDraft, saveIndicatorDraft } from '@/api/kpi';
import type { IndicatorVersionInput, IndicatorVersionSummary } from '@/api/kpi/types';

const version = ref<IndicatorVersionInput>();
const versions = ref<IndicatorVersionSummary[]>([]);
const selectedVersion = ref('2026-v1');
const selectedSummary = computed(() => versions.value.find(item => item.version === selectedVersion.value));
const dialogVisible = ref(false);
const saving = ref(false);
const form = reactive<IndicatorVersionInput>({ version: '', formulaVersion: 'weighted-score-v1', scoreScale: 4, indicators: [] });
const references = ref<Array<{ type: string; code: string; status: string }>>([]);

async function load() {
  try {
    versions.value = (await listIndicatorVersions()).data || [];
    if (versions.value.length && !versions.value.some(item => item.version === selectedVersion.value)) selectedVersion.value = versions.value[0].version;
    await selectVersion(selectedVersion.value);
  } catch { version.value = undefined; }
}
async function selectVersion(code: string) { selectedVersion.value = code; const summary = versions.value.find(item => item.version === code); references.value = (await listIndicatorReferences(code)).data || []; version.value = summary?.status === 'DRAFT' ? { version: code, formulaVersion: summary.formulaVersion, scoreScale: summary.scoreScale, indicators: (await getIndicatorVersionItems(code)).data || [] } : (await getIndicatorVersion(code)).data; }
function openCreate() { form.version = ''; form.formulaVersion = 'weighted-score-v1'; form.scoreScale = 4; form.indicators = [{ code: '', name: '', weight: 0, maximumScore: 100 }]; dialogVisible.value = true; }
function openEdit() { if (!version.value) return; form.version = version.value.version; form.formulaVersion = version.value.formulaVersion; form.scoreScale = version.value.scoreScale; form.indicators = version.value.indicators.map(item => ({ ...item })); dialogVisible.value = true; }
async function saveDraft() {
  if (!form.version || !form.indicators.length || form.indicators.some(item => !item.code || !item.name || item.weight <= 0)) { ElMessage.warning('请完整填写版本和指标项'); return; }
  saving.value = true;
  try { await saveIndicatorDraft(form); ElMessage.success('指标草稿已保存'); dialogVisible.value = false; await load(); } finally { saving.value = false; }
}
async function publishDraftVersion() { await publishIndicatorDraft(selectedVersion.value); ElMessage.success('指标版本发布成功'); await load(); }
async function removeDraft() { await deleteIndicatorDraft(selectedVersion.value); ElMessage.success('指标草稿已删除'); await load(); }
async function cloneVersion() { const result = await ElMessageBox.prompt('请输入新版本编码，例如 2026-v2', '复制指标版本', { inputPattern: /^[A-Za-z0-9._-]+$/, inputErrorMessage: '只能包含字母、数字、点、下划线和短横线' }); await cloneIndicatorVersion(selectedVersion.value, result.value); ElMessage.success('已复制为草稿版本'); await load(); selectedVersion.value = result.value; await selectVersion(result.value); }
onMounted(load);
</script>

<style scoped>
.toolbar{display:flex;align-items:center;justify-content:space-between;font-size:18px;font-weight:600}.version-tabs{margin-top:16px}.version-actions{margin-top:16px}.mt-4{margin-top:16px}.items{width:100%}.item-row{display:grid;grid-template-columns:1fr 1.5fr 120px 120px auto;gap:8px;margin-bottom:8px}
</style>
