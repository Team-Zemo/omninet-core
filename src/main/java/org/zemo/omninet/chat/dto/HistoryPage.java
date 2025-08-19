package org.zemo.omninet.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryPage {
    private List<MessageView> items;
    private int page; private int size;
    private boolean hasMore;
}

