<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header><div class="toolbar"><span>绩效方案管理</span><el-button type="primary" @click="dialog = true">新增方案</el-button></div></template>
      <el-table :data="schemes" border><el-table-column prop="code" label="方案编码" width="160" /><el-table-column prop="name" label="方案名称" /><el-table-column prop="category" label="适用组织" width="150" /><el-table-column prop="status" label="状态" width="120" /><el-table-column label="操作" width="220"><template #default="{ row }"><template v-if="row.status === 'DRAFT'"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="success" @click="publish(row.code)">发布</el-button><el-button link type="danger" @click="remove(row.code)">删除</el-button></template></template></el-table-column></el-table>
      <el-empty v-if="!schemes.length" description="暂无绩效方案，请新增方案" />
    </el-card>
    <el-dialog v-model="dialog" title="新增绩效方案" width="560px"><el-form :model="form" label-width="110px"><el-form-item label="方案编码"><el-input v-model="form.schemeCode" placeholder="例如 COLLEGE-2026" /></el-form-item><el-form-item label="方案名称"><el-input v-model="form.schemeName" /></el-form-item><el-form-item label="适用组织"><el-select v-model="form.organizationCategory"><el-option label="二级学院" value="二级学院" /><el-option label="职能部门" value="职能部门" /></el-select></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createPerformanceScheme, deletePerformanceScheme, listPerformanceSchemes, publishPerformanceScheme, updatePerformanceScheme, type PerformanceSchemeVO } from '@/api/kpi';
const schemes = ref<PerformanceSchemeVO[]>([]);
const dialog = ref(false); const editing = ref(false); const form = reactive({ schemeCode: '', schemeName: '', organizationCategory: '二级学院' });
async function load() { schemes.value = (await listPerformanceSchemes()).data || []; }
function edit(row: PerformanceSchemeVO) { editing.value = true; form.schemeCode = row.code; form.schemeName = row.name; form.organizationCategory = row.category; dialog.value = true; }
async function save() { if (!form.schemeCode || !form.schemeName) { ElMessage.warning('请填写方案编码和名称'); return; } if (editing.value) await updatePerformanceScheme(form.schemeCode, { name: form.schemeName, category: form.organizationCategory }); else await createPerformanceScheme({ code: form.schemeCode, name: form.schemeName, category: form.organizationCategory }); editing.value = false; dialog.value = false; ElMessage.success('方案已保存'); await load(); }
async function remove(code: string) { await deletePerformanceScheme(code); ElMessage.success('方案已删除'); await load(); }
async function publish(code: string) { await publishPerformanceScheme(code); ElMessage.success('方案已发布'); await load(); }
onMounted(load);
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:600}</style>
