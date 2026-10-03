package com.tourism.business.notifications.service;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
@Component
public class MailTemplates {
    public record Mail(String subject,String text) {}
    private final ObjectMapper mapper;
    public MailTemplates(ObjectMapper mapper) { this.mapper=mapper; }
    public Mail render(String type,String locale,String payload) {
        var p=mapper.readTree(payload); boolean en="en".equals(locale),pt="pt".equals(locale);
        if(type.equals("CASH_CLOSED")){String subject=en?"Cash closure":pt?"Fechamento de caixa":"Cierre de caja";return new Mail(subject+" #"+p.path("closureId").asText(),subject+" #"+p.path("closureId").asText()+" · "+p.path("businessDate").asText()+"\nCRC · America/Costa_Rica\n"+(en?"PDF and Excel reports attached.":pt?"Relatórios PDF e Excel anexados.":"Se adjuntan los reportes PDF y Excel."));}
        String title=type.equals("RESERVATION_CHANGED")?(en?"Reservation updated":pt?"Reserva atualizada":"Reserva actualizada"):type.equals("HOURS_CHANGED")?(en?"Opening hours changed":pt?"Alteração de horários":"Cambio de horarios"):type.equals("RESERVATION_CANCELLED")?(en?"Reservation cancelled":pt?"Reserva cancelada":"Reserva cancelada"):(en?"Reservation confirmed":pt?"Reserva confirmada":"Reserva confirmada");
        var text=new StringBuilder(title).append("\n").append(p.path("businessName").asText("")).append("\n");
        text.append(en?"Code: ":pt?"Código: ":"Código: ").append(p.path("code").asText()).append("\n");
        JsonNode q=p.has("quote")?p.get("quote"):p;
        text.append(en?"Visit date: ":pt?"Data da visita: ":"Fecha de visita: ").append(q.path("visitDate").asText()).append("\n");
        if(p.has("name")) text.append(en?"Name: ":pt?"Nome: ":"Nombre: ").append(p.path("name").asText()).append("\n");
        if(q.has("hours")) { text.append(en?"Opening hours: ":pt?"Horários: ":"Horarios: ");for(var h:q.path("hours")) text.append(h.path("opensAt").asText()).append("–").append(h.path("closesAt").asText()).append(" ");text.append("\n"); }
        if(q.has("lines")) {
            String[] labels=en?new String[]{"Adults (11–64)","Children (0–10)","Seniors (65+)","Vehicles"}:pt?new String[]{"Adultos (11–64)","Crianças (0–10)","Idosos (65+)","Veículos"}:new String[]{"Adultos (11–64)","Niños (0–10)","Adultos mayores (65+)","Vehículos"};
            int i=0;for(var line:q.path("lines")) { text.append(labels[i++]).append(": ").append(line.path("quantity").asInt()).append(" × ").append(line.path("unitPrice").asText()).append(" CRC = ").append(line.path("subtotal").asText()).append(" CRC\n"); }
            text.append("Total: ").append(q.path("total").asText()).append(" CRC\n");
            text.append(en?"Pay on arrival or transfer by SINPE to ":pt?"Pague na chegada ou por SINPE para ":"Paga al llegar o transfiere por SINPE al ").append(p.path("sinpeNumber").asText()).append(".\n");
            text.append(en?"Transfers are verified manually; payment is recorded when you arrive.\n":pt?"As transferências são verificadas manualmente; o pagamento é registrado na chegada.\n":"Las transferencias se verifican manualmente; el pago se registra al llegar.\n");
        }
        text.append(en?"For changes or cancellation, contact the business: ":pt?"Para alterações ou cancelamento, entre em contato: ":"Para cambios o cancelación, contacta al negocio: ").append(p.path("phone").asText("")).append(" ").append(p.path("email").asText("")).append("\n");
        text.append(en?"Thank you!":pt?"Obrigado!":"¡Gracias!");
        return new Mail(title+" · "+p.path("code").asText(),text.toString());
    }
}
