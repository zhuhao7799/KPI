<template>
  <div class="page p-2">
    <el-card shadow="never"><template #header><div class="heading"><div><h1>绩效审核待办</h1><p>仅显示当前登录人有权办理的任务</p></div><div class="toolbar"><el-input v-model="assigneeId" placeholder="审核人账号" clearable /><el-button type="primary" :loading="assigning" @click="assignFirst">指派首条待办</el-button><el-button :loading="loading" @click="refresh">刷新</el-button></div></div></template>
      <el-table :data="tasks" v-loading="loading" empty-text="暂无待办任务">
        <el-table-column prop="id" label="任务编号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="submissionId" label="填报编号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="type" label="任务类型" width="110" />
        <el-table-column label="操作" width="170"><template #default="scope"><el-button link type="success" @click="decide(scope.row.id, true)">通过</el-button><el-button link type="danger" @click="decide(scope.row.id, false)">退回</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup name="PerformanceTasks" lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { assignReview, completeTask, listTasks } from '@/api/kpi';
import type { ApprovalTaskVO } from '@/api/kpi/types';

const tasks = ref<ApprovalTaskVO[]>([]);
const loading = ref(false);
const assigning = ref(false);
const assigneeId = ref('local-reviewer');
async function refresh() {
  loading.value = true;
  try { tasks.value = (await listTasks()).data || []; }
  finally { loading.value = false; }
}
async function decide(taskId: string, approved: boolean) {
  try {
    const comment = await ElMessageBox.prompt('请填写审核意见', approved ? '确认通过' : '确认退回', { inputValidator: value => Boolean(value?.trim()), inputErrorMessage: '审核意见不能为空' });
    await completeTask(taskId, approved, comment.value.trim());
    ElMessage.success('任务已办理');
    await refresh();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error;
  }
}
async function assignFirst() {
  const task = tasks.value[0];
  if (!task || !assigneeId.value.trim()) return;
  assigning.value = true;
  try { await assignReview(task.submissionId, assigneeId.value.trim()); ElMessage.success('待办已指派'); await refresh(); }
  finally { assigning.value = false; }
}
onMounted(refresh);
</script>
<style scoped>
.page .el-card{border-radius:12px}.heading{display:flex;justify-content:space-between;align-items:center;gap:16px}.heading h1{margin:0;color:#183b60;font-size:24px}.heading p{margin:8px 0 0;color:#789;font-size:13px}.toolbar{display:flex;align-items:center;gap:8px}.toolbar .el-input{width:170px}@media(max-width:700px){.heading,.toolbar{align-items:stretch;flex-direction:column}.toolbar .el-input{width:100%}}
</style>
