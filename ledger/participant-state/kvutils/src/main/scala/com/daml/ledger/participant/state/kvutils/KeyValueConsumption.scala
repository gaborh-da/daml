package com.daml.ledger.participant.state.kvutils

import com.daml.ledger.participant.state.kvutils.Conversions._
import com.daml.ledger.participant.state.kvutils.DamlKvutils._
import com.daml.ledger.participant.state.v1.{
  CommittedTransaction,
  RejectionReason,
  SubmittedTransaction,
  TransactionMeta,
  Update
}
import com.digitalasset.daml.lf.data.Time.Timestamp
import com.google.common.io.BaseEncoding

object KeyValueConsumption {

  /** Construct a participant-state [[Update]] from a [[DamlLogEntry]].
    *
    * This method is expected to be used to implement [[com.daml.ledger.participant.state.v1.ReadService.stateUpdates]].
    *
    * @param entryId: The log entry identifier.
    * @param entry: The log entry.
    * @return [[[Update]] constructed from log entry.
    */
  def logEntryToUpdate(entryId: DamlLogEntryId, entry: DamlLogEntry): Update = {

    val recordTime = parseTimestamp(entry.getRecordTime)

    entry.getPayloadCase match {
      case DamlLogEntry.PayloadCase.ARCHIVE =>
        Update.PublicPackageUploaded(entry.getArchive)

      case DamlLogEntry.PayloadCase.TRANSACTION_ENTRY =>
        txEntryToUpdate(entryId, entry.getTransactionEntry, recordTime)

      case DamlLogEntry.PayloadCase.CONFIGURATION_ENTRY =>
        Update.ConfigurationChanged(parseDamlConfigurationEntry(entry.getConfigurationEntry))

      case DamlLogEntry.PayloadCase.REJECTION_ENTRY =>
        rejEntryToUpdate(entryId, entry.getRejectionEntry, recordTime)

      case DamlLogEntry.PayloadCase.PAYLOAD_NOT_SET =>
        throw new RuntimeException("entryToUpdate: Payload is not set!")
    }
  }

  private def rejEntryToUpdate(
      entryId: DamlLogEntryId,
      rejEntry: DamlRejectionEntry,
      recordTime: Timestamp): Update.CommandRejected = {

    Update.CommandRejected(
      submitterInfo = parseSubmitterInfo(rejEntry.getSubmitterInfo),
      reason = RejectionReason.Inconsistent // FIXME
    )
  }

  /** Transform the transaction entry into the [[Update.TransactionAccepted]] event. */
  private def txEntryToUpdate(
      entryId: DamlLogEntryId,
      txEntry: DamlTransactionEntry,
      recordTime: Timestamp): Update.TransactionAccepted = {
    val relTx = Conversions.decodeTransaction(txEntry.getTransaction)
    val hexTxId = BaseEncoding.base16.encode(entryId.toByteArray)

    Update.TransactionAccepted(
      optSubmitterInfo = Some(parseSubmitterInfo(txEntry.getSubmitterInfo)),
      transactionMeta = TransactionMeta(
        ledgerEffectiveTime = parseTimestamp(txEntry.getLedgerEffectiveTime),
        workflowId = txEntry.getWorkflowId,
      ),
      transaction = makeCommittedTransaction(entryId, relTx),
      transactionId = hexTxId,
      recordTime = recordTime,
      referencedContracts = List.empty // TODO(JM): rename this to additionalContracts. Always empty here.
    )
  }

  private def makeCommittedTransaction(
      txId: DamlLogEntryId,
      tx: SubmittedTransaction): CommittedTransaction = {
    tx
    /* Assign absolute contract ids */
      .mapContractIdAndValue(
        toAbsCoid(txId, _),
        _.mapContractId(toAbsCoid(txId, _))
      )
  }

}