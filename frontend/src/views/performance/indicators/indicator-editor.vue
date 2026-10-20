<template>
  <div class="app-container indicator-editor-page">
    <el-card shadow="never">
      <template #header><div class="page-head"><div><div class="eyebrow">KPI GOVERNANCE</div><h2>{{ isCreate ? '新增指标' : '编辑指标' }}</h2><p>版本：{{ versionCode }}，单独维护一个指标的完整属性</p></div><el-button @click="back">返回版本明细</el-button></div></template>
      <el-form :model="form" label-width="110px" class="editor-form">
        <el-form-item label="指标编码"><el-input v-model="form.code" placeholder="例如：goal" /></el-form-item>
        <el-form-item label="指标名称"><el-input v-model="form.name" placeholder="请输入指标名称" /></el-form-item>
        <el-form-item label="指标分类"><el-select v-model="form.category"><el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="指标性质"><el-select v-model="form.indicatorType"><el-option v-for="item in types" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="计量单位"><el-select v-model="form.measurementUnit"><el-option v-for="item in units" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="权重"><el-input-number v-model="form.weight" :min="0.01" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="满分"><el-input-number v-model="form.maximumScore" :min="1" :precision="2" /></el-form-item>
        <el-form-item label="评分方式"><el-select v-model="form.scoringMethod"><el-option v-for="item in methods" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="佐证材料"><el-checkbox v-model="form.evidenceRequired">需要材料</el-checkbox></el-form-item>
        <el-form-item label="指标说明"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <div class="actions"><el-button @click="back">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存指标</el-button></div>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref, toRefs } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { addIndicatorItem, getIndicatorVersionItems, updateIndicatorItem } from '@/api/kpi';
import { useDict } from '@/utils/dict';
const route = useRoute(); const router = useRouter(); const saving = ref(false);
const versionCode = String(route.query.version || ''); const targetCode = String(route.query.indicator || ''); const isCreate = route.query.mode === 'create';
const form = reactive<any>({ code:'', name:'', weight:0, maximumScore:100, category:'综合管理', indicatorType:'定量', measurementUnit:'分', scoringMethod:'加权评分', evidenceRequired:false, description:'' });
const { kpi_indicator_category: categories, kpi_indicator_type: types, kpi_measurement_unit: units, kpi_scoring_method: methods } = toRefs<any>(useDict('kpi_indicator_category','kpi_indicator_type','kpi_measurement_unit','kpi_scoring_method'));
async function load(){ if(!versionCode)return; const items=(await getIndicatorVersionItems(versionCode)).data || []; const item=items.find((x:any)=>x.code===targetCode); if(item) Object.assign(form,{...item,weight:Number(item.weight ?? 0),maximumScore:Number(item.maximumScore ?? 100)}); }
function back(){ router.push({path:'/performance/indicators/detail',query:{version:versionCode}}); }
async function save(){ if(!form.code.trim()||!form.name.trim()||form.weight<=0){ElMessage.warning('请完整填写指标编码、名称和权重');return;} saving.value=true; try{ if(isCreate) await addIndicatorItem(versionCode,form); else await updateIndicatorItem(versionCode,targetCode,form); ElMessage.success('指标已保存');back(); } finally{saving.value=false;} }
onMounted(load);
</script>
<style scoped>
.indicator-editor-page{max-width:980px;margin:0 auto}.page-head{display:flex;justify-content:space-between;align-items:flex-start;gap:20px}.eyebrow{color:var(--el-color-primary);font-size:11px;font-weight:700;letter-spacing:1.6px}.page-head h2{margin:8px 0 4px}.page-head p{margin:0;color:var(--el-text-color-secondary)}.editor-form{max-width:760px;padding:24px 12px}.editor-form :deep(.el-input),.editor-form :deep(.el-select),.editor-form :deep(.el-input-number){width:100%}.actions{display:flex;justify-content:flex-end;gap:10px;padding-top:18px;border-top:1px solid var(--el-border-color-lighter)}
</style>
