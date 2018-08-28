package ele.flow

import co.paralleluniverse.fibers.Suspendable
import ele.Commands
import ele.contract.AssetDocContract
import ele.contract.AssetTraceStateContract
import ele.dto.AssetDoc
import ele.state.AssetDocState
import ele.state.AssetTraceState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 计划实现:
 * 1. 资产有扩展表, 附件表
 * 2. 通过交易实现.
 *
 * 仍然是购买方向卖家发出购买资产的标识, 有卖家将AssetTraceState作为input
 *
 * Created by lydon on 2018/8/27.
 */
object AssetTraceFlow {

    /**
     * 添加资产信息
     */
    @InitiatingFlow
    @StartableByRPC
    class AddAsset(private val asset: AssetDoc) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {

            val myParty = ourIdentity
            val defaultNotary = serviceHub.networkMapCache.notaryIdentities[0]

            val txBuilder = TransactionBuilder(defaultNotary)

            txBuilder.withItems(
                    // 添加踪迹
                    StateAndContract(
                            AssetTraceState(asset.assetNo, 1, myParty, asset.userCardId, asset.userCardName, "", ""),
                            AssetTraceStateContract.CONTRACT_ID
                    ),
                    StateAndContract(
                            AssetDocState(
                                    asset,
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                    myParty),
                            AssetDocContract.CONTRACT_ID
                    ),
                    Command(Commands.Create(), myParty.owningKey)
            )

            txBuilder.verify(serviceHub)
            val signedTransaction = serviceHub.signInitialTransaction(txBuilder)
            return subFlow(FinalityFlow(signedTransaction))
        }
    }
}