<template>
  <div class="hd">
    <!-- 身份栏：角色由后端再次校验，不是只靠隐藏按钮 -->
    <div class="eq-panel hd__identity">
      <div class="hd__identity-left">
        <span class="hd-id-badge">操作人</span>
        <span class="eq-muted">当前操作人</span>
        <el-input v-model="operator.name" placeholder="操作人姓名" style="width: 170px" />
        <el-select v-model="operator.role" style="width: 150px">
          <el-option label="普通工作人员" value="STAFF" />
          <el-option label="负责人" value="MANAGER" />
        </el-select>
        <el-tag v-if="isManager()" type="danger" effect="dark" size="small">可复训放行</el-tag>
        <el-tag v-else type="info" effect="plain" size="small">仅登记 / 处置 / 复查</el-tag>
      </div>
      <el-button text type="primary" @click="loadAll">刷新全部</el-button>
    </div>

    <div class="hd__body">
      <!-- 左：事件列表 -->
      <div class="eq-panel hd__list">
        <div class="eq-panel__head">
          <h2>健康事件</h2>
          <el-button type="primary" @click="openRegister">登记健康事件</el-button>
        </div>

        <div class="eq-toolbar">
          <el-select v-model="filters.horseId" clearable placeholder="全部马匹" style="width: 160px" @change="reloadEvents">
            <el-option
              v-for="horse in horses"
              :key="horse.id"
              :label="horse.horseNo + ' ' + horse.name"
              :value="horse.id"
            />
          </el-select>
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 130px" @change="reloadEvents">
            <el-option label="待处理" value="PENDING" />
            <el-option label="观察中" value="OBSERVING" />
            <el-option label="待复查" value="REVIEW_PENDING" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
          <el-checkbox v-model="filters.overdue" border @change="reloadEvents">只看逾期未闭环</el-checkbox>
        </div>

        <div v-if="loadingEvents" class="eq-empty">加载中…</div>
        <div v-else-if="!events.length" class="eq-empty">没有符合条件的健康事件</div>

        <div class="hd__events">
          <div v-for="event in events" :key="event.id" class="hd-event" @click="openEvent(event.id)">
            <div class="hd-event__top">
              <div>
                <span class="hd-event__no">{{ event.eventNo }}</span>
                <b class="hd-event__horse">{{ event.horseName }}（{{ event.horseNo }}）</b>
              </div>
              <div class="hd-event__tags">
                <el-tag :type="severityTag(event.severity)" effect="dark" size="small">
                  {{ event.severityName }}
                </el-tag>
                <el-tag :type="statusTag(event.status)" size="small">{{ event.statusName }}</el-tag>
                <el-tag v-if="event.overdue" type="danger" size="small">复查逾期</el-tag>
                <el-tag v-else-if="event.passExpired" type="warning" size="small">合格结论过期</el-tag>
              </div>
            </div>
            <div class="hd-event__symptom">{{ event.symptom }}</div>
            <div class="hd-event__meta">
              <span>发生 {{ formatDateTime(event.occurredAt) }}</span>
              <span>下次复查 {{ event.nextReviewDate || '—' }}</span>
              <span v-if="event.passReviewAt">合格复查 {{ formatDateTime(event.passReviewAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：马匹健康概览（未闭环事件 + 最近复查结论 + 放行） -->
      <div class="eq-panel hd__overview">
        <div class="eq-panel__head">
          <h2>马匹风险概览</h2>
        </div>
        <el-select
          v-model="overviewHorseId"
          filterable
          placeholder="选择马匹查看未闭环事件"
          style="width: 100%; margin-bottom: 12px"
          @change="reloadOverview"
        >
          <el-option
            v-for="horse in horses"
            :key="horse.id"
            :label="`${horse.horseNo} ${horse.name}（${horse.statusName}）`"
            :value="horse.id"
          />
        </el-select>

        <div v-if="!overview" class="eq-empty">选择一匹马查看健康风险</div>
        <template v-else>
          <div class="hd-risk">
            <el-tag :type="horseStatusTag(overview.horseStatus)" effect="dark">
              {{ overview.horseStatusName }}
            </el-tag>
            <span :class="{ 'hd-risk__num--danger': overview.openCount > 0 }" class="hd-risk__num">
              未闭环 {{ overview.openCount }}
            </span>
            <span :class="{ 'hd-risk__num--danger': overview.overdueCount > 0 }" class="hd-risk__num">
              逾期 {{ overview.overdueCount }}
            </span>
          </div>

          <div class="hd-block">
            <h4>最近一次合格复查</h4>
            <div v-if="overview.latestPass" class="hd-pass">
              <p>{{ overview.latestPass.passConclusion || '—' }}</p>
              <span class="eq-muted">
                {{ overview.latestPass.eventNo }} · {{ formatDateTime(overview.latestPass.passReviewAt) }}
                <el-tag v-if="overview.latestPass.passExpired" type="warning" size="small">已过期</el-tag>
              </span>
            </div>
            <div v-else class="eq-muted">暂无合格复查结论</div>
          </div>

          <div class="hd-block">
            <h4>未闭环事件（{{ overview.openEvents.length }}）</h4>
            <div v-if="!overview.openEvents.length" class="eq-muted">没有未闭环事件</div>
            <div
              v-for="event in overview.openEvents"
              :key="event.id"
              class="hd-open"
              @click="openEvent(event.id)"
            >
              <div>
                <b>{{ event.eventNo }}</b>
                <el-tag :type="severityTag(event.severity)" size="small" style="margin-left: 6px">
                  {{ event.severityName }}
                </el-tag>
                <el-tag :type="statusTag(event.status)" size="small" style="margin-left: 4px">
                  {{ event.statusName }}
                </el-tag>
              </div>
              <div class="eq-muted">
                {{ event.passConclusion ? '合格待放行' : '尚未合格复查' }} · 复查日 {{ event.nextReviewDate || '—' }}
                <el-tag v-if="event.overdue" type="danger" size="small">逾期</el-tag>
              </div>
            </div>
          </div>

          <div class="hd-block">
            <h4>复训放行</h4>
            <template v-if="overview.clearanceBlockers.length">
              <div class="hd-blocker" v-for="(blocker, index) in overview.clearanceBlockers" :key="index">
                <span class="hd-blocker__icon">!</span>{{ blocker }}
              </div>
            </template>
            <p v-else class="hd-ok">健康条件已满足：全部未闭环事件均有有效期内的合格复查，可由负责人放行。</p>
            <el-button
              type="danger"
              style="width: 100%; margin-top: 8px"
              :disabled="!isManager() || !overview.canClear"
              @click="openClearance"
            >
              {{ isManager() ? '负责人确认复训放行' : '仅负责人可放行' }}
            </el-button>
          </div>
        </template>
      </div>
    </div>

    <!-- 登记健康事件 -->
    <el-dialog v-model="registerDialog" title="登记健康事件" width="560px">
      <el-form label-width="104px">
        <el-form-item label="马匹" required>
          <el-select v-model="registerForm.horseId" filterable style="width: 100%" placeholder="选择马匹">
            <el-option
              v-for="horse in horses"
              :key="horse.id"
              :label="`${horse.horseNo} ${horse.name}（${horse.statusName}）`"
              :value="horse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker
            v-model="registerForm.occurredAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="不填默认现在"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="严重程度" required>
          <el-radio-group v-model="registerForm.severity">
            <el-radio-button label="LOW">低风险</el-radio-button>
            <el-radio-button label="MEDIUM">中风险</el-radio-button>
            <el-radio-button label="HIGH">高风险</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="registerForm.severity === 'HIGH'"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 12px"
          title="高风险事件登记后，该马匹立即进入休养，不能再被安排到新课程；已有的未来排期会标记为受影响（保留不删除）。"
        />
        <el-form-item label="症状说明" required>
          <el-input v-model="registerForm.symptom" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="处置建议" required>
          <el-input v-model="registerForm.treatmentAdvice" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="预计复查日" required>
          <el-date-picker v-model="registerForm.nextReviewDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRegister">登记</el-button>
      </template>
    </el-dialog>

    <!-- 事件详情抽屉：事件链时间线 + 受影响排期 + 处置动作 -->
    <el-drawer v-model="eventDrawer" size="560px" :title="activeEvent ? `${activeEvent.eventNo} · ${activeEvent.horseName}` : ''">
      <template v-if="activeEvent">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="马匹">
            {{ activeEvent.horseName }}（{{ activeEvent.horseNo }}）· {{ activeEvent.horseStatusName }}
          </el-descriptions-item>
          <el-descriptions-item label="严重程度">
            <el-tag :type="severityTag(activeEvent.severity)" effect="dark" size="small">
              {{ activeEvent.severityName }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(activeEvent.status)" size="small">{{ activeEvent.statusName }}</el-tag>
            <el-tag v-if="activeEvent.overdue" type="danger" size="small" style="margin-left: 6px">复查逾期</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ formatDateTime(activeEvent.occurredAt) }}</el-descriptions-item>
          <el-descriptions-item label="症状">{{ activeEvent.symptom }}</el-descriptions-item>
          <el-descriptions-item label="处置建议">{{ activeEvent.treatmentAdvice }}</el-descriptions-item>
          <el-descriptions-item label="下次复查日">{{ activeEvent.nextReviewDate || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="activeEvent.passConclusion" label="最近合格复查">
            {{ activeEvent.passConclusion }}（{{ formatDateTime(activeEvent.passReviewAt) }}）
            <el-tag v-if="activeEvent.passExpired" type="warning" size="small">已过期</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 操作区（已关闭事件不能再处置） -->
        <div v-if="activeEvent.status !== 'CLOSED'" class="hd-actions">
          <el-button size="small" @click="openAction('PROCESS')">补充处置</el-button>
          <el-button v-if="activeEvent.status === 'PENDING'" size="small" type="primary" @click="openAction('START_OBSERVE')">
            开始观察
          </el-button>
          <el-button v-if="activeEvent.status === 'OBSERVING'" size="small" type="primary" @click="openAction('REQUEST_REVIEW')">
            申请复查
          </el-button>
          <template v-if="activeEvent.status === 'REVIEW_PENDING'">
            <el-button size="small" @click="openAction('REVIEW_CONTINUE')">继续观察 / 改复查日</el-button>
            <el-button size="small" type="success" @click="openAction('REVIEW_PASS')">复查合格</el-button>
          </template>
        </div>
        <el-alert
          v-else
          type="info"
          :closable="false"
          style="margin: 12px 0"
          :title="`事件已于 ${formatDateTime(activeEvent.closedAt)} 随复训放行关闭，处置历史保留不可改写`"
        />

        <!-- 受影响排期（可追溯，不删除） -->
        <h4 class="hd-section-title">受影响排期追溯（{{ activeEvent.affectedSessions.length }}）</h4>
        <div v-if="!activeEvent.affectedSessions.length" class="eq-muted">登记时没有需要标记的未来排期</div>
        <div v-for="row in activeEvent.affectedSessions" :key="row.linkId" class="hd-session">
          <div>
            <b>{{ row.sessionDate }} {{ row.startTime }}</b> {{ row.lessonName }} · {{ row.coachName }}教练
            <el-tag size="small" effect="plain">{{ row.statusName }}</el-tag>
            <el-tag v-if="row.stillAffected" type="danger" size="small">仍受影响</el-tag>
            <el-tag v-else type="success" size="small">已放行恢复</el-tag>
          </div>
          <div class="eq-muted">标记于 {{ formatDateTime(row.markedAt) }}（排期保留，未删除）</div>
        </div>

        <!-- append-only 流转历史 -->
        <h4 class="hd-section-title">处置历史（只追加，不可改写）</h4>
        <el-timeline>
          <el-timeline-item
            v-for="row in [...activeEvent.transitions].reverse()"
            :key="row.id"
            :type="row.action === 'REVIEW_PASS' ? 'success' : row.action === 'CLOSE' ? 'danger' : 'primary'"
            :timestamp="`${formatDateTime(row.operatedAt)} · ${row.operatorName}（${row.operatorRoleName}）`"
          >
            <b>{{ row.actionName }}</b>
            <span v-if="row.fromStatusName" class="eq-muted">
              ：{{ row.fromStatusName }} → {{ row.toStatusName }}
            </span>
            <p v-if="row.note" class="hd-timeline-note">{{ row.note }}</p>
            <span v-if="row.nextReviewDate" class="eq-muted">下次复查日 {{ row.nextReviewDate }}</span>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>

    <!-- 处置 / 观察 / 复查动作弹窗 -->
    <el-dialog v-model="actionDialog" :title="actionMeta.title" width="500px">
      <el-form label-width="104px">
        <el-alert
          v-if="actionStale"
          type="error"
          :closable="false"
          show-icon
          title="该事件在你打开后已被其他人更新，请关闭并重新载入最新事件链。"
          style="margin-bottom: 12px"
        />
        <el-form-item label="操作人">
          <el-input :model-value="`${operator.name}（${operator.role === 'MANAGER' ? '负责人' : '普通工作人员'}）`" disabled />
        </el-form-item>
        <el-form-item v-if="actionForm.action === 'REVIEW_PASS'" label="合格结论" required>
          <el-input v-model="actionForm.conclusion" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="如：跛行消失，慢步快步正常，建议安排复训" />
        </el-form-item>
        <el-form-item v-else :label="actionMeta.noteLabel" required>
          <el-input v-model="actionForm.note" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item v-if="actionMeta.needDate" label="下次复查日" required>
          <el-date-picker v-model="actionForm.nextReviewDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-alert
          v-if="actionForm.action === 'REVIEW_PASS'"
          type="info"
          :closable="false"
          :title="`合格结论 ${PASS_VALID_DAYS} 天内有效；事件不会自动关闭，需负责人复训放行后马匹才恢复在役。`"
        />
      </el-form>
      <template #footer>
        <el-button @click="actionDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="actionStale" @click="submitAction">提交</el-button>
      </template>
    </el-dialog>

    <!-- 负责人复训放行 -->
    <el-dialog v-model="clearDialog" title="负责人复训放行" width="500px">
      <el-form label-width="104px">
        <el-form-item label="马匹">
          <el-input :model-value="overview ? `${overview.horseName}（${overview.horseNo}）` : ''" disabled />
        </el-form-item>
        <el-alert
          v-if="clearanceStale"
          type="error"
          :closable="false"
          show-icon
          title="你打开放行窗口后这匹马的健康事件链发生了变化，请取消并重新载入后再确认。"
          style="margin-bottom: 12px"
        />
        <el-form-item label="放行说明" required>
          <el-input v-model="clearForm.note" type="textarea" :rows="2" maxlength="500" show-word-limit
            placeholder="负责人确认意见，将写入每条事件的关闭历史" />
        </el-form-item>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="确认后将关闭全部未闭环事件并把马匹恢复为在役；若仍有未合格事件、复查过期、马匹已退役或结论基于旧事件，后端会拒绝。"
        />
      </el-form>
      <template #footer>
        <el-button @click="clearDialog = false">取消</el-button>
        <el-button type="danger" :loading="saving" :disabled="clearanceStale" @click="submitClearance">确认放行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { operator, isManager, newRequestKey } from '../api/identity'
import { pendingHealthSelection } from '../router'

const PASS_VALID_DAYS = 3

const horses = ref([])
const events = ref([])
const loadingEvents = ref(false)
const overview = ref(null)
const overviewHorseId = ref(null)

const filters = reactive({ horseId: null, status: '', overdue: false })
const saving = ref(false)

const registerDialog = ref(false)
const eventDrawer = ref(false)
const actionDialog = ref(false)
const clearDialog = ref(false)
const activeEvent = ref(null)
const actionStale = ref(false)

const registerForm = reactive(emptyRegister())
const actionForm = reactive(emptyAction())
const actionMeta = reactive({ title: '', noteLabel: '处置说明', needDate: false })
const clearForm = reactive({ note: '', chainVersion: null, requestKey: '' })

function emptyRegister() {
  return {
    horseId: null,
    occurredAt: '',
    severity: 'MEDIUM',
    symptom: '',
    treatmentAdvice: '',
    nextReviewDate: ''
  }
}

function emptyAction() {
  return { action: '', note: '', conclusion: '', nextReviewDate: '', chainVersion: null, requestKey: '' }
}

const ACTION_META = {
  PROCESS: { title: '补充处置', noteLabel: '处置说明', needDate: false },
  START_OBSERVE: { title: '开始观察', noteLabel: '观察说明', needDate: true },
  REQUEST_REVIEW: { title: '申请复查', noteLabel: '复查申请说明', needDate: true },
  REVIEW_CONTINUE: { title: '复查后继续观察', noteLabel: '复查说明', needDate: true },
  REVIEW_PASS: { title: '复查合格', noteLabel: '复查说明', needDate: true }
}

const clearanceStale = computed(() =>
  Boolean(overview.value && clearForm.chainVersion !== null && overview.value.chainVersion !== clearForm.chainVersion)
)

function severityTag(severity) {
  if (severity === 'HIGH') return 'danger'
  if (severity === 'MEDIUM') return 'warning'
  return 'info'
}

function statusTag(status) {
  if (status === 'PENDING') return 'warning'
  if (status === 'OBSERVING') return 'primary'
  if (status === 'REVIEW_PENDING') return 'danger'
  return 'success'
}

function horseStatusTag(status) {
  if (status === 'ACTIVE') return 'success'
  if (status === 'RESTING') return 'warning'
  return 'info'
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadHorses() {
  horses.value = (await api.horses()) || []
}

async function reloadEvents() {
  loadingEvents.value = true
  try {
    events.value =
      (await api.healthEvents({
        horseId: filters.horseId || undefined,
        status: filters.status || undefined,
        overdue: filters.overdue ? true : undefined
      })) || []
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loadingEvents.value = false
  }
}

async function reloadOverview() {
  if (!overviewHorseId.value) {
    overview.value = null
    return
  }
  try {
    overview.value = await api.horseHealthOverview(overviewHorseId.value, operator.role)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function loadAll() {
  await Promise.all([loadHorses(), reloadEvents()])
  if (overviewHorseId.value) {
    await reloadOverview()
  }
}

function openRegister() {
  Object.assign(registerForm, emptyRegister())
  if (filters.horseId) {
    registerForm.horseId = filters.horseId
  }
  registerDialog.value = true
}

async function submitRegister() {
  if (!registerForm.horseId) return ElMessage.warning('请选择马匹')
  if (!registerForm.symptom.trim()) return ElMessage.warning('请填写症状说明')
  if (!registerForm.treatmentAdvice.trim()) return ElMessage.warning('请填写处置建议')
  if (!registerForm.nextReviewDate) return ElMessage.warning('请选择预计复查日')
  if (!operator.name.trim()) return ElMessage.warning('请先在顶部填写操作人姓名')
  saving.value = true
  try {
    const created = await api.registerHealthEvent({
      horseId: registerForm.horseId,
      occurredAt: registerForm.occurredAt || undefined,
      severity: registerForm.severity,
      symptom: registerForm.symptom.trim(),
      treatmentAdvice: registerForm.treatmentAdvice.trim(),
      nextReviewDate: registerForm.nextReviewDate,
      operatorName: operator.name.trim(),
      operatorRole: operator.role,
      requestKey: newRequestKey()
    })
    ElMessage.success('健康事件已登记')
    registerDialog.value = false
    overviewHorseId.value = created.horseId
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function openEvent(id) {
  try {
    activeEvent.value = await api.healthEvent(id)
    eventDrawer.value = true
    if (!overviewHorseId.value) {
      overviewHorseId.value = activeEvent.value.horseId
      await reloadOverview()
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openAction(action) {
  if (!activeEvent.value) return
  const latest = activeEvent.value
  Object.assign(actionForm, {
    ...emptyAction(),
    action,
    nextReviewDate: latest.nextReviewDate || '',
    chainVersion: latest.chainVersion,
    requestKey: newRequestKey()
  })
  Object.assign(actionMeta, ACTION_META[action])
  actionStale.value = false
  actionDialog.value = true
}

async function submitAction() {
  if (!operator.name.trim()) return ElMessage.warning('请先在顶部填写操作人姓名')
  if (actionForm.action !== 'PROCESS' && !actionForm.nextReviewDate) {
    return ElMessage.warning('请选择下次复查日')
  }
  if (actionForm.action === 'REVIEW_PASS' && !actionForm.conclusion.trim()) {
    return ElMessage.warning('请填写合格复查结论')
  }
  if (actionForm.action !== 'REVIEW_PASS' && !actionForm.note.trim()) {
    return ElMessage.warning('请填写说明')
  }
  saving.value = true
  try {
    const payload = {
      action: actionForm.action,
      note: actionForm.note.trim() || actionForm.conclusion.trim(),
      conclusion: actionForm.conclusion.trim() || undefined,
      nextReviewDate: actionForm.nextReviewDate || undefined,
      chainVersion: actionForm.chainVersion,
      operatorName: operator.name.trim(),
      operatorRole: operator.role,
      requestKey: actionForm.requestKey
    }
    const updated = await api.transitionHealthEvent(activeEvent.value.id, payload)
    ElMessage.success('处置已记录')
    actionDialog.value = false
    eventDrawer.value = false
    activeEvent.value = updated
    await loadAll()
  } catch (error) {
    await handleSubmitError(error)
  } finally {
    saving.value = false
  }
}

function openClearance() {
  if (!overview.value) return
  clearForm.note = ''
  clearForm.chainVersion = overview.value.chainVersion
  clearForm.requestKey = newRequestKey()
  clearDialog.value = true
}

async function submitClearance() {
  if (!operator.name.trim()) return ElMessage.warning('请先在顶部填写操作人姓名')
  if (!clearForm.note.trim()) return ElMessage.warning('请填写放行说明')
  saving.value = true
  try {
    const result = await api.clearHealth({
      horseId: overviewHorseId.value,
      note: clearForm.note.trim(),
      chainVersion: clearForm.chainVersion,
      operatorName: operator.name.trim(),
      operatorRole: operator.role,
      requestKey: clearForm.requestKey
    })
    ElMessage.success(result.message || '复训放行已确认')
    clearDialog.value = false
    await loadAll()
  } catch (error) {
    await handleSubmitError(error)
  } finally {
    saving.value = false
  }
}

/**
 * 并发冲突（409）：不覆盖最新状态，提示数据已变化并引导重新载入；
 * 403：权限不足（普通工作人员放行）；其余为业务拒绝原因。
 */
async function handleSubmitError(error) {
  if (error.status === 409) {
    actionStale.value = true
    try {
      await ElMessageBox.alert(error.message, '数据已变化', {
        confirmButtonText: '重新载入最新事件链',
        type: 'warning'
      })
    } catch (cancel) {
      // 用户关掉弹窗，动作弹窗上的过期提示仍在
    }
    if (activeEvent.value) {
      try {
        activeEvent.value = await api.healthEvent(activeEvent.value.id)
      } catch (reloadError) {
        // 忽略二次加载失败
      }
    }
    await Promise.all([reloadEvents(), reloadOverview(), loadHorses()])
    return
  }
  ElMessage.error(error.message)
}

onMounted(async () => {
  await loadHorses()
  if (pendingHealthSelection.horseId !== null) {
    overviewHorseId.value = pendingHealthSelection.horseId
    filters.horseId = pendingHealthSelection.horseId
    pendingHealthSelection.horseId = null
  }
  await Promise.all([reloadEvents(), reloadOverview()])
})

// 切换「当前身份」角色后重算放行权限（后端仍会再次校验，这里只同步界面）
watch(
  () => operator.role,
  () => {
    if (overviewHorseId.value) {
      reloadOverview()
    }
  }
)
</script>

<style scoped>
.hd-id-badge {
  background: #f3e9e4;
  color: #6d4c41;
  border: 1px solid #e3d3cb;
  border-radius: 6px;
  padding: 2px 8px;
  font-size: 12px;
}

.hd__identity {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.hd__identity-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hd__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.hd__events {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hd-event {
  border: 1px solid #ece3df;
  border-radius: 10px;
  padding: 12px;
  background: #fffdfc;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.hd-event:hover {
  border-color: #cbb4a9;
  box-shadow: 0 2px 10px rgba(109, 76, 65, 0.12);
}

.hd-event__top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.hd-event__no {
  font-size: 12px;
  color: #a08d86;
  letter-spacing: 1px;
  margin-right: 8px;
}

.hd-event__horse {
  color: #4a3129;
  font-size: 14px;
}

.hd-event__tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.hd-event__symptom {
  margin: 8px 0 4px;
  font-size: 13px;
  color: #5d4f49;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hd-event__meta {
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: #9a8b85;
  flex-wrap: wrap;
}

.hd-risk {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.hd-risk__num {
  font-size: 13px;
  color: #4a3129;
}

.hd-risk__num--danger {
  color: #c45656;
  font-weight: 700;
}

.hd-block {
  border-top: 1px dashed #efe6e1;
  padding-top: 12px;
  margin-top: 12px;
}

.hd-block h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #4a3129;
}

.hd-pass p {
  margin: 0 0 4px;
  font-size: 13px;
}

.hd-open {
  padding: 8px;
  border: 1px solid #f0e6e1;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.hd-open:hover {
  border-color: #cbb4a9;
}

.hd-blocker {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: #b1534f;
  margin-bottom: 6px;
}

.hd-blocker__icon {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #c45656;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  margin-top: 1px;
}

.hd-ok {
  font-size: 12px;
  color: #4f8a5b;
  margin: 0;
}

.hd-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 14px 0;
}

.hd-section-title {
  margin: 18px 0 10px;
  font-size: 13px;
  color: #4a3129;
}

.hd-session {
  border: 1px solid #f0e6e1;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  font-size: 12px;
}

.hd-timeline-note {
  margin: 4px 0 2px;
  font-size: 12px;
  color: #5d4f49;
}
</style>
