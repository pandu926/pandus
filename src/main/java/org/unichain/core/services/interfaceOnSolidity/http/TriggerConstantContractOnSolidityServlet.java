package org.unichain.core.services.interfaceOnSolidity.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.unichain.core.services.http.fullnode.servlet.TriggerConstantContractServlet;
import org.unichain.core.services.interfaceOnSolidity.WalletOnSolidity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Slf4j(topic = "API")
public class TriggerConstantContractOnSolidityServlet extends TriggerConstantContractServlet {

  @Autowired
  private WalletOnSolidity walletOnSolidity;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> super.doGet(request, response));
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    walletOnSolidity.futureGet(() -> {
      try {
        super.doPost(request, response);
      } catch (IOException e) {
        logger.error("TriggerConstantContractOnSolidityServlet Exception", e);
      }
    });
  }
}
