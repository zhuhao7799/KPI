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
        <el-table-column label="操作" width="140"><template #default="{ row }"><el-button v-if="row.state !== 'LOCKED'" link type="primary" @click="lock(row.code)">锁定周期</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="新增考核周期" width="520px">
      <el-form :model="form" label-width="100px"><el-form-item label="周期编码"><el-input v-model="form.code" placeholder="例如 2027" /></el-form-item><el-form-item label="周期名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="form.startsOn" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.endsOn" type="date" value-format="YYYY-MM-DD" /></el-form-item></el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createCycle, listCycles, lockCycle } from '@/api/kpi';
import type { AssessmentCycleVO } from '@/api/kpi/types';
const cycles = ref<AssessmentCycleVO[]>([]); const dialog = ref(false); const form = reactive<AssessmentCycleVO>({ code: '', name: '', startsOn: '', endsOn: '', state: 'DRAFT' });
async function load() { cycles.value = (await listCycles()).data || []; }
async function save() { await createCycle(form); ElMessage.success('周期创建成功'); dialog.value = false; await load(); }
async function lock(code: string) { await lockCycle(code); ElMessage.success('周期已锁定'); await load(); }
onMounted(load);
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center;font-size:18px;font-weight:600}</style>
