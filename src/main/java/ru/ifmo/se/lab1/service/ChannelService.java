package ru.ifmo.se.lab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.lab1.domain.Channel;
import ru.ifmo.se.lab1.domain.User;
import ru.ifmo.se.lab1.dto.ChannelDto;
import ru.ifmo.se.lab1.dto.CreateChannelRequest;
import ru.ifmo.se.lab1.exception.ResourceNotFoundException;
import ru.ifmo.se.lab1.repository.ChannelRepository;

@Service
@RequiredArgsConstructor
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final UserService userService;

    @Transactional
    public ChannelDto createChannel(CreateChannelRequest request) {
        User user = userService.getDomainUser(request.getUserId());
        Channel channel = new Channel();
        channel.setUser(user);
        channel.setName(request.getName());
        channel.setDescription(request.getDescription());
        channel = channelRepository.save(channel);
        return mapToDto(channel);
    }

    @Transactional(readOnly = true)
    public ChannelDto getChannel(Long id) {
        return mapToDto(channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found")));
    }

    public Channel getDomainChannel(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));
    }

    private ChannelDto mapToDto(Channel channel) {
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setUserId(channel.getUser().getId());
        dto.setName(channel.getName());
        dto.setDescription(channel.getDescription());
        dto.setCreatedAt(channel.getCreatedAt());
        return dto;
    }
}
