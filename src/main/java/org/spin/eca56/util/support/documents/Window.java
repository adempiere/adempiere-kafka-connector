/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 or later of the                                  *
 * GNU General Public License as published                                    *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2023 E.R.P. Consultores y Asociados, C.A.               *
 * All Rights Reserved.                                                       *
 * Contributor(s): Yamel Senih www.erpya.com                                  *
 *****************************************************************************/
package org.spin.eca56.util.support.documents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_Element;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_Window;
import org.adempiere.model.MBrowse;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MForm;
import org.compiere.model.MProcess;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.wf.MWorkflow;
import org.spin.eca56.util.support.DictionaryDocument;
import org.spin.util.ASPUtil;

/**
 * 	the document class for Window senders
 * 	@author Yamel Senih, ysenih@erpya.com, ERPCyA http://www.erpya.com
 */
public class Window extends DictionaryDocument {

	//	Some default documents key
	public static final String KEY = "new";
	public static final String CHANNEL = "window";
	
	@Override
	public String getKey() {
		return KEY;
	}
	
	@Override
	public DictionaryDocument withEntity(PO entity) {
		MWindow window = (MWindow) entity;
		Map<String, Object> documentDetail = new HashMap<>();
		documentDetail.put("id", window.getAD_Window_ID());
		documentDetail.put("uuid", window.getUUID());
		documentDetail.put("name", window.get_Translation(I_AD_Window.COLUMNNAME_Name, getLanguage()));
		documentDetail.put("description", window.get_Translation(I_AD_Window.COLUMNNAME_Description, getLanguage()));
		documentDetail.put("help", window.get_Translation(I_AD_Window.COLUMNNAME_Help, getLanguage()));
		documentDetail.put("window_type", window.getWindowType());
		documentDetail.put("is_sales_transaction", window.isSOTrx());
		//	Tabs
		documentDetail.put("tabs", convertTabs(Arrays.asList(window.getTabs(false, null))));
		putDocument(documentDetail);
		return this;
	}

	private Map<String, Object> parseDictionaryEntity(PO entity) {
		Map<String, Object> documentEntity = new HashMap<>();
		documentEntity.put("id", entity.get_ID());
		documentEntity.put("uuid", entity.get_UUID());
		documentEntity.put("name", entity.get_Translation(I_AD_Element.COLUMNNAME_Name, getLanguage()));
		documentEntity.put("description", entity.get_Translation(I_AD_Element.COLUMNNAME_Description, getLanguage()));
		documentEntity.put("help", entity.get_Translation(I_AD_Element.COLUMNNAME_Help, getLanguage()));
		return documentEntity;
	}

	private List<Map<String, Object>> convertTabs(List<MTab> tabs) {
		List<Map<String, Object>> tabsDetail = new ArrayList<>();
		if(tabs == null || tabs.isEmpty()) {
			return tabsDetail;
		}
		tabs.forEach(tab -> {
			tabsDetail.add(parseTab(tab));
		});
		return tabsDetail;
	}
	
	private Map<String, Object> parseTab(MTab tab) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("id", tab.getAD_Tab_ID());
		detail.put("uuid", tab.getUUID());
		detail.put("name", tab.get_Translation(I_AD_Tab.COLUMNNAME_Name, getLanguage()));
		detail.put("description", tab.get_Translation(I_AD_Tab.COLUMNNAME_Description, getLanguage()));
		detail.put("help", tab.get_Translation(I_AD_Tab.COLUMNNAME_Help, getLanguage()));
		// Record attributes
		detail.put("is_insert_record", tab.isInsertRecord());
		detail.put("commit_warning", tab.get_Translation(I_AD_Tab.COLUMNNAME_CommitWarning, getLanguage()));
		// Attributes
		detail.put("display_logic", tab.getDisplayLogic());
		detail.put("sequence", tab.getSeqNo());
		detail.put("tab_level", tab.getTabLevel());
		detail.put("is_read_only", tab.isReadOnly());
		detail.put("read_only_logic", tab.getReadOnlyLogic());
		detail.put("is_single_row", tab.isSingleRow());
		detail.put("is_advanced_tab", tab.isAdvancedTab());
		detail.put("is_has_tree", tab.isHasTree());
		detail.put("is_info_tab", tab.isInfoTab());
		detail.put("is_translation_tab", tab.isTranslationTab());

		// Table attributes
		if(tab.getAD_Table_ID() > 0) {
			MTable table = new MTable(tab.getCtx(), tab.getAD_Table_ID(), null);
			detail.put("table_name", table.getTableName());

			Map<String, Object> referenceDetail = new HashMap<>();
			referenceDetail.put("table_name", table.getTableName());
			referenceDetail.put("access_level", table.getAccessLevel());
			referenceDetail.put("key_columns",
				Arrays.asList(
					table.getKeyColumns()
				)
			);
			referenceDetail.put("is_view", table.isView());
			referenceDetail.put("is_document", table.isDocument());
			referenceDetail.put("is_deleteable", table.isDeleteable());
			referenceDetail.put("is_change_log", table.isChangeLog());
			List<String> identifierColumns = table.getColumnsAsList(false).stream()
				.filter(column -> {
					return column.isIdentifier();
				})
				.sorted(Comparator.comparing(MColumn::getSeqNo))
				.map(column -> {
					return column.getColumnName();
				})
				.collect(Collectors.toList())
			;
			referenceDetail.put("identifier_columns", identifierColumns);
			List<String> selectionColums = table.getColumnsAsList(false).stream()
				.filter(column -> {
					return column.isSelectionColumn();
				})
				.map(column -> {
					return column.getColumnName();
				})
				.collect(Collectors.toList())
			;
			referenceDetail.put("selection_colums", selectionColums);
			detail.put("table", referenceDetail);
		}

		// Link attributes
		List<String> contextColumnsList = ReferenceUtil.getContextColumnNames(
			Optional.ofNullable(tab.getWhereClause()).orElse("")
			+ Optional.ofNullable(tab.getOrderByClause()).orElse("")
		);
		detail.put("context_column_names", contextColumnsList);
		if(tab.getParent_Column_ID() > 0) {
			//	Parent Link Column Name
			MColumn column = MColumn.get(tab.getCtx(), tab.getParent_Column_ID());
			detail.put("parent_column_name", column.getColumnName());
		}
		if(tab.getAD_Column_ID() > 0) {
			//	Link Column Name
			MColumn column = MColumn.get(tab.getCtx(), tab.getAD_Column_ID());
			detail.put("link_column_name", column.getColumnName());
		}
		// Sort attributes
		detail.put("is_sort_tab", tab.isSortTab());
		if (tab.isSortTab()) {
			//	Sort Column
			if(tab.getAD_ColumnSortOrder_ID() > 0) {
				MColumn column = MColumn.get(tab.getCtx(), tab.getAD_ColumnSortOrder_ID());
				detail.put("sort_order_column_name", column.getColumnName());
			}
			//	Sort Yes / No
			if(tab.getAD_ColumnSortYesNo_ID() > 0) {
				MColumn column = MColumn.get(tab.getCtx(), tab.getAD_ColumnSortYesNo_ID());
				detail.put("sort_yes_no_column_name", column.getColumnName());
			}
		}

		// External info
		detail.put("window_id", tab.getAD_Window_ID());
		detail.put("process_id", tab.getAD_Process_ID());
		if(tab.getAD_Process_ID() > 0) {
			MProcess process = MProcess.get(tab.getCtx(), tab.getAD_Process_ID());
			if (process.isActive()) {
				Map<String, Object> referenceDetail = parseProcess(process);
				detail.put("process", referenceDetail);
			}
		}
		detail.put("processes", convertProcesses(getProcessFromTab(tab)));
		
		//	Fields
		List<MField> fields = Arrays.asList(tab.getFields(false, null));
		detail.put("fields", convertFields(fields));
		detail.put("row_fields", convertFields(fields.stream().filter(field -> field.isDisplayed()).collect(Collectors.toList())));
		detail.put("grid_fields", convertFields(fields.stream().filter(field -> field.isDisplayedGrid()).collect(Collectors.toList())));
		//	Processes
		return detail;
	}

	private List<MProcess> getProcessFromTab(MTab tab) {
		final String whereClause = "IsActive = 'Y' AND ("
				// first process on tab
				+ "AD_Process_ID = ? " // #1
				// process on column
				+ "OR EXISTS("
					+ "SELECT 1 FROM AD_Field f "
					+ "INNER JOIN AD_Column c ON(c.AD_Column_ID = f.AD_Column_ID) "
					+ "WHERE c.AD_Process_ID = AD_Process.AD_Process_ID "
					+ "AND f.IsDisplayed = 'Y' "
					+ "AND f.AD_Tab_ID = ? " // #2
					+ "AND f.IsActive = 'Y'"
				+ ") "
				// process on table
				+ "OR EXISTS("
					+ "SELECT 1 FROM AD_Table_Process AS tp "
					+ "WHERE tp.AD_Process_ID = AD_Process.AD_Process_ID "
					+ "AND tp.AD_Table_ID = ? " // #3
					+ "AND tp.IsActive = 'Y'"
				+ ")"
			+ ")"
		;

		List<Object> filterList = new ArrayList<>();
		filterList.add(tab.getAD_Process_ID());
		filterList.add(tab.getAD_Tab_ID());
		filterList.add(tab.getAD_Table_ID());

		return new Query(
			tab.getCtx(),
			I_AD_Process.Table_Name,
			whereClause,
			null
		)
			.setParameters(filterList)
			.list();
	}
	
	private List<Map<String, Object>> convertFields(List<MField> fields) {
		List<Map<String, Object>> fieldsDetail = new ArrayList<>();
		if(fields == null) {
			return fieldsDetail;
		}
		fields.forEach(field -> {
			fieldsDetail.add(parseField(field));
		});
		return fieldsDetail;
	}
	
	private List<Map<String, Object>> convertProcesses(List<MProcess> processesList) {
		List<Map<String, Object>> processesDetail = new ArrayList<>();
		if(processesList == null || processesList.isEmpty()) {
			return processesDetail;
		}
		processesList.forEach(process -> {
			processesDetail.add(
				parseProcess(process)
			);
		});
		return processesDetail;
	}
	
	private Map<String, Object> parseProcess(MProcess process) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("id", process.getAD_Process_ID());
		detail.put("uuid", process.getUUID());
		detail.put("name", process.get_Translation(I_AD_Process.COLUMNNAME_Name, getLanguage()));
		detail.put("description", process.get_Translation(I_AD_Process.COLUMNNAME_Description, getLanguage()));
		detail.put("help", process.get_Translation(I_AD_Process.COLUMNNAME_Help, getLanguage()));
		detail.put("is_report", process.isReport());

		// Linked
		detail.put("browser_id", process.getAD_Browse_ID());
		detail.put("form_id", process.getAD_Form_ID());
		detail.put("workflow_id", process.getAD_Workflow_ID());
		if (process.getAD_Browse_ID() > 0) {
			MBrowse browse = ASPUtil.getInstance(process.getCtx()).getBrowse(process.getAD_Browse_ID());
			detail.put("browse", parseDictionaryEntity(browse));
		} else if (process.getAD_Form_ID() > 0) {
			MForm form = new MForm(process.getCtx(), process.getAD_Workflow_ID(), null);
			detail.put("form", parseDictionaryEntity(form));
		} else if (process.getAD_Workflow_ID() > 0) {
			MWorkflow workflow = MWorkflow.get(process.getCtx(), process.getAD_Workflow_ID());
			detail.put("workflow", parseDictionaryEntity(workflow));
		}
		return detail;
	}

	private Map<String, Object> parseField(MField field) {
		Map<String, Object> detail = new HashMap<>();
		detail.put("id", field.getAD_Field_ID());
		detail.put("uuid", field.getUUID());
		detail.put("name", field.get_Translation(I_AD_Field.COLUMNNAME_Name, getLanguage()));
		detail.put("description", field.get_Translation(I_AD_Field.COLUMNNAME_Description, getLanguage()));
		detail.put("help", field.get_Translation(I_AD_Field.COLUMNNAME_Help, getLanguage()));

		//
		detail.put("is_allow_copy", field.isAllowCopy());
		detail.put("is_heading", field.isHeading());
		detail.put("is_field_only", field.isFieldOnly());

		//	Column Properties
		MColumn column = MColumn.get(field.getCtx(), field.getAD_Column_ID());
		detail.put("column_name", column.getColumnName());
		detail.put("column_sql", column.getColumnSQL());
		detail.put("is_key", column.isKey());
		detail.put("is_translated", column.isTranslated());
		detail.put("is_identifier", column.isIdentifier());
		detail.put("identifier_sequence", column.getSeqNo());
		detail.put("is_selection_column", column.isSelectionColumn());
		detail.put("callout", column.getCallout());

		int displayTypeId = field.getAD_Reference_ID();
		if(displayTypeId <= 0) {
			displayTypeId = column.getAD_Reference_ID();
		}
		detail.put("display_type", displayTypeId);
		
		//	Value Properties
		detail.put("default_value", Optional.ofNullable(field.getDefaultValue()).orElse(column.getDefaultValue()));
		detail.put("field_length", column.getFieldLength());
		detail.put("v_format", column.getVFormat());
		detail.put("format_pattern", column.getFormatPattern());
		detail.put("value_min", column.getValueMin());
		detail.put("value_max", column.getValueMax());
		detail.put("is_encrypted", field.isEncrypted());

		//	Display Properties
		detail.put("is_displayed", field.isDisplayed());
		detail.put("display_logic", field.getDisplayLogic());
		detail.put("sequence", field.getSeqNo());
		detail.put("is_displayed_grid", field.isDisplayedGrid());
		detail.put("grid_sequence", field.getSeqNoGrid());

		//	Editable Properties
		detail.put("is_read_only", field.isReadOnly());
		detail.put("read_only_logic", column.getReadOnlyLogic());
		detail.put("is_updateable", column.isUpdateable());
		detail.put("is_always_updateable", column.isAlwaysUpdateable());

		//	Mandatory Properties
		detail.put("is_mandatory", (field.getIsMandatory() != null && field.getIsMandatory().equals("Y")? true: column.isMandatory()));
		detail.put("mandatory_logic", column.getMandatoryLogic());

		//	External Info
		int referenceValueId = field.getAD_Reference_Value_ID();
		if(referenceValueId <= 0) {
			referenceValueId = column.getAD_Reference_Value_ID();
		}
		int validationRuleId = field.getAD_Val_Rule_ID();
		if(validationRuleId <= 0) {
			validationRuleId = column.getAD_Val_Rule_ID();
		}
		String embeddedContextColumn = null;
		ReferenceValues referenceValues = ReferenceUtil.getReferenceDefinition(column.getColumnName(), displayTypeId, referenceValueId, validationRuleId);
		if(referenceValues != null) {
			// Map<String, Object> referenceDetail = new HashMap<>();
			// referenceDetail.put("id", referenceValues.getReferenceId());
			// referenceDetail.put("table_name", referenceValues.getTableName());
			// detail.put("display_type", referenceDetail);
			embeddedContextColumn = referenceValues.getEmbeddedContextColumn();
		}
		detail.put("context_column_names", ReferenceUtil.getContextColumnNames(
			Optional.ofNullable(field.getDefaultValue()).orElse(column.getDefaultValue())
			+ Optional.ofNullable(field.getDisplayLogic()).orElse("")
			+ Optional.ofNullable(column.getMandatoryLogic()).orElse("")
			+ Optional.ofNullable(column.getReadOnlyLogic()).orElse("")
			+ Optional.ofNullable(embeddedContextColumn).orElse(""))
		);
		detail.put("dependent_fields", DependenceUtil.generateDependentWindowFields(field));
		detail.put("process_id", column.getAD_Process_ID());
		if (column.getAD_Process_ID() > 0) {
			detail.put("process", parseProcess(
				MProcess.get(field.getCtx(), column.getAD_Process_ID())
			));
		}
		return detail;
	}
	
	private Window() {
		super();
	}
	
	/**
	 * Default instance
	 * @return
	 */
	public static Window newInstance() {
		return new Window();
	}

	@Override
	public String getChannel() {
		return CHANNEL;
	}
}
