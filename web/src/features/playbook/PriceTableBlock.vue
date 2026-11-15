<script setup lang="ts">
import { computed } from 'vue'
import type { Installments, Plan } from './api'
import { fieldKey, minCash, useEditor } from './editor'
import FieldMessage from './FieldMessage.vue'
import InstallmentsInput from './InstallmentsInput.vue'
import { LIMITS, playbookMessages as msg } from './messages'
import { formatCash, formatInstallments, toCents } from './money'
import MoneyInput from './MoneyInput.vue'

const editor = useEditor()
const { editing, incomplete, readOnly } = editor
const table = computed(() => editor.draft.value!.priceTable)
const plans = computed(() => table.value.plans)
const full = computed(() => plans.value.length >= LIMITS.plans)

const editingTable = computed(
  () => editing.value === 'extraColumn' || plans.value.some((p) => p.id === editing.value),
)
const showExtra = computed(
  () => editingTable.value || !!table.value.extraColumnName || plans.value.some((p) => p.extraValue),
)

const DISCOUNT = fieldKey(null, 'maxDiscount')

function setCash(plan: Plan, value: string | null) {
  const key = fieldKey(plan.id, 'cash')
  editor.setLocalError(key, null)
  if (value === plan.cash) return
  plan.cash = value
  plan.review = null
  void editor.save(key)
}

function setInstallments(plan: Plan, value: Installments | null) {
  const key = fieldKey(plan.id, 'installments')
  editor.setLocalError(key, null)
  if (JSON.stringify(value) === JSON.stringify(plan.installments)) return
  plan.installments = value
  plan.review = null
  void editor.save(key)
}

function setDiscount(value: string | null) {
  const min = minCash(editor.draft.value!)
  if (value !== null && min !== null && toCents(value) >= toCents(min)) {
    editor.setLocalError(DISCOUNT, msg.discount(min))
    return
  }
  editor.setLocalError(DISCOUNT, null)
  if (value === table.value.maxDiscount) return
  table.value.maxDiscount = value
  table.value.maxDiscountReview = null
  void editor.save(DISCOUNT)
}

function confirmPlan(plan: Plan) {
  plan.review = null
  void editor.save()
}

function confirmDiscount() {
  table.value.maxDiscountReview = null
  void editor.save()
}
</script>

<template>
  <section class="card" aria-label="Tabela de preços">
    <h2 class="lbl">Tabela de preços</h2>
    <table class="tbl">
      <thead>
        <tr>
          <th>Plano</th>
          <th
            v-if="showExtra"
            data-row="extraColumn"
            @click="editor.startEdit('extraColumn')"
            @focusout="editor.leaveRow($event, 'extraColumn')"
          >
            <input
              v-if="editingTable"
              v-model="table.extraColumnName"
              class="f"
              placeholder="Coluna extra"
              aria-label="Nome da coluna extra"
              @blur="editor.commitText(table, 'extraColumnName', fieldKey(null, 'extraColumnName'))"
            />
            <template v-else>{{ table.extraColumnName }}</template>
            <FieldMessage :field="fieldKey(null, 'extraColumnName')" />
          </th>
          <th>À vista</th>
          <th>Parcelado</th>
          <th class="w-px"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="plan in plans"
          :key="plan.id"
          :data-row="plan.id"
          class="row cursor-pointer"
          :class="{ editing: editing === plan.id, incomplete: incomplete.has(plan.id) }"
          :data-incomplete="incomplete.has(plan.id) || undefined"
          @click="editor.startEdit(plan.id)"
          @focusout="editor.leaveRow($event, plan.id)"
        >
          <td>
            <input
              v-if="editing === plan.id"
              v-model="plan.name"
              class="f"
              aria-label="Plano"
              @blur="editor.commitText(plan, 'name', fieldKey(plan.id, 'name'))"
            />
            <template v-else>{{ plan.name }}</template>
            <FieldMessage :field="fieldKey(plan.id, 'name')" />
            <div v-if="plan.review" class="mt-1 grid justify-items-start gap-1">
              <span class="tag amb">Importado · conferir</span>
              <span class="text-[12px] text-muted-foreground">{{ plan.review.excerpt }}</span>
              <button type="button" class="btn xs" :disabled="readOnly" @click.stop="confirmPlan(plan)">Conferido</button>
            </div>
          </td>
          <td v-if="showExtra">
            <input
              v-if="editing === plan.id"
              v-model="plan.extraValue"
              class="f"
              :aria-label="table.extraColumnName || 'Coluna extra'"
              @blur="editor.commitText(plan, 'extraValue', fieldKey(plan.id, 'extraValue'))"
            />
            <template v-else>{{ plan.extraValue }}</template>
            <FieldMessage :field="fieldKey(plan.id, 'extraValue')" />
          </td>
          <td class="font-mono">
            <MoneyInput
              v-if="editing === plan.id"
              :value="plan.cash"
              label="À vista"
              @commit="setCash(plan, $event)"
              @invalid="editor.setLocalError(fieldKey(plan.id, 'cash'), msg.cash)"
            />
            <template v-else-if="plan.cash">{{ formatCash(plan.cash) }}</template>
            <FieldMessage :field="fieldKey(plan.id, 'cash')" />
          </td>
          <td class="font-mono">
            <InstallmentsInput
              v-if="editing === plan.id"
              :value="plan.installments"
              @commit="setInstallments(plan, $event)"
              @invalid="editor.setLocalError(fieldKey(plan.id, 'installments'), msg.installments)"
            />
            <template v-else>{{ formatInstallments(plan.installments) }}</template>
            <FieldMessage :field="fieldKey(plan.id, 'installments')" />
          </td>
          <td>
            <button type="button" class="row-tools btn xs" :disabled="readOnly" @click.stop="editor.removeItem(plans, plan.id)">Remover</button>
          </td>
        </tr>
      </tbody>
    </table>
    <button
      type="button"
      class="btn sm mt-2.5"
      :disabled="readOnly || full"
      @click="editor.addItem(plans, { name: '', extraValue: '', cash: null, installments: null, review: null })"
    >
      + Plano
    </button>
    <p v-if="full" class="mt-1 text-[12.5px] text-muted-foreground">{{ msg.blockLimit(LIMITS.plans) }}</p>
    <div
      v-if="plans.length"
      data-row="discount"
      class="row mt-2 cursor-pointer text-[12.5px] text-muted-foreground"
      @click="editor.startEdit('discount')"
      @focusout="editor.leaveRow($event, 'discount')"
    >
      <div class="flex flex-wrap items-center gap-1.5">
        Desconto máximo na call:
        <span v-if="editing === 'discount'" class="w-32">
          <MoneyInput
            :value="table.maxDiscount"
            label="Desconto máximo na call"
            allow-zero
            @commit="setDiscount"
            @invalid="editor.setLocalError(DISCOUNT, msg.discount(minCash(editor.draft.value!) ?? '0.00'))"
          />
        </span>
        <span v-else-if="table.maxDiscount !== null" class="font-mono text-foreground">{{ formatCash(table.maxDiscount) }}</span>
        <span v-else class="tag amb">a definir</span>
        <template v-if="table.maxDiscountReview">
          <span class="tag amb">Importado · conferir</span>
          <span>{{ table.maxDiscountReview.excerpt }}</span>
          <button type="button" class="btn xs" :disabled="readOnly" @click.stop="confirmDiscount">Conferido</button>
        </template>
      </div>
      <FieldMessage :field="DISCOUNT" />
    </div>
  </section>
</template>
