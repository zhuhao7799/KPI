<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header><div class="toolbar"><span>绩效方案管理</span><el-button type="primary" @click="dialog = true">新增方案</el-button></div></template>
      <el-table :data="schemes" border><el-table-column prop="schemeCode" label="方案编码" width="160" /><el-table-column prop="schemeName" label="方案名称" /><el-table-column prop="organizationCategory" label="适用组织" width="150" /><el-table-column prop="status" label="状态" width="120" /></el-table>
      <el-empty v-if="!schemes.length" description="暂无绩效方案，请新增方案" />
    </el-card>
    <el-dialog v-model="dialog" title="新增绩效方案" width="560px"><el-form :model="form" label-width="110px"><el-form-item label="方案编码"><el-input v-model="form.schemeCode" placeholder="例如 COLLEGE-2026" /></el-form-item><el-form-item label="方案名称"><el-input v-model="form.schemeName" /></el-form-item><el-form-item label="适用组织"><el-select v-model="form.organizationCategory"><el-option label="二级学院" value="二级学院" /><el-option label="职能部门" value="职能部门" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createPerformanceScheme, listPerformanceSchemes, type PerformanceSchemeVO } from '@/api/kpi';
const schemes = ref<PerformanceSchemeVO[]>([]);
const dialog = ref(false); const form = reactive({ schemeCode: '', schemeName: '', organizationCategory: '二级学院' });
async function load() { schemes.value = (await listPerformanceSchemes()).data || []; }
async function save() { if (!form.schemeCode || !form.schemeName) { ElMessage.warning('请填写方案编码和名称'); return; } await createPerformanceScheme({ code: form.schemeCode, name: form.schemeName, category: form.organizationCategory }); dialog.value = false; ElMessage.success('方案草稿已保存'); await load(); }
onMounted(load);
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:600}</style>
